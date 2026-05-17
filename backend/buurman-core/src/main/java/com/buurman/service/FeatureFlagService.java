package com.buurman.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.buurman.domain.EvaluatedFlag;
import com.buurman.domain.FeatureFlag;
import com.buurman.domain.FeatureFlagOverride;
import com.buurman.domain.OverrideScope;
import com.buurman.domain.SegmentContext;
import com.buurman.domain.TeamRole;
import com.buurman.repository.CalendarFeedRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.FeatureFlagOverrideRepository;
import com.buurman.repository.FeatureFlagRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PhotoRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.FeatureFlags;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FeatureFlagService {

  private final FeatureFlagRepository flagRepo;
  private final FeatureFlagOverrideRepository overrideRepo;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final ContactRepository contactRepository;
  private final PhotoRepository photoRepository;
  private final DocumentRepository documentRepository;
  private final ExpenseRepository expenseRepository;
  private final PaymentRepository paymentRepository;
  private final CalendarFeedRepository calendarFeedRepository;
  private final SegmentEvaluator segmentEvaluator;
  private final Clock clock;
  private final MetricsService metricsService;
  private final MeterRegistry registry;

  private final LoadingCache<String, List<FeatureFlag>> flagDefsCache;
  private final Cache<OverrideCacheKey, List<FeatureFlagOverride>> overrideCache;
  private Optional<Instant> lastInvalidatedAt = Optional.empty();

  public FeatureFlagService(
      FeatureFlagRepository flagRepo,
      FeatureFlagOverrideRepository overrideRepo,
      TeamRepository teamRepository,
      TeamMemberRepository teamMemberRepository,
      TeamPreferencesRepository teamPreferencesRepository,
      PropertyRepository propertyRepository,
      ContractRepository contractRepository,
      ContactRepository contactRepository,
      PhotoRepository photoRepository,
      DocumentRepository documentRepository,
      ExpenseRepository expenseRepository,
      PaymentRepository paymentRepository,
      CalendarFeedRepository calendarFeedRepository,
      SegmentEvaluator segmentEvaluator,
      Clock clock,
      MetricsService metricsService,
      MeterRegistry registry) {
    this.flagRepo = flagRepo;
    this.overrideRepo = overrideRepo;
    this.teamRepository = teamRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.teamPreferencesRepository = teamPreferencesRepository;
    this.propertyRepository = propertyRepository;
    this.contractRepository = contractRepository;
    this.contactRepository = contactRepository;
    this.photoRepository = photoRepository;
    this.documentRepository = documentRepository;
    this.expenseRepository = expenseRepository;
    this.paymentRepository = paymentRepository;
    this.calendarFeedRepository = calendarFeedRepository;
    this.segmentEvaluator = segmentEvaluator;
    this.clock = clock;
    this.metricsService = metricsService;
    this.registry = registry;

    this.flagDefsCache =
        Caffeine.newBuilder()
            .maximumSize(1)
            .refreshAfterWrite(60, TimeUnit.SECONDS)
            .recordStats()
            .build(key -> flagRepo.findAll());

    this.overrideCache =
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .recordStats()
            .build();
  }

  @PostConstruct
  void warmCacheAndValidate() {
    try {
      List<FeatureFlag> flags = flagRepo.findAll();
      flagDefsCache.put("all", flags);
      Set<String> dbKeys = flags.stream().map(FeatureFlag::getKey).collect(Collectors.toSet());
      FeatureFlags.ALL_KEYS.stream()
          .filter(k -> !dbKeys.contains(k))
          .forEach(k -> log.warn("Feature flag '{}' defined in code but missing from DB", k));
    } catch (Exception e) {
      log.warn("Failed to warm feature flag cache on startup", e);
    }
    metricsService.registerGauge(
        "featureflag.cache.flag_definitions.size", flagDefsCache, c -> c.estimatedSize());
    metricsService.registerGauge(
        "featureflag.cache.overrides.size", overrideCache, c -> c.estimatedSize());
    CaffeineCacheMetrics.monitor(registry, flagDefsCache, "flag_definitions");
    CaffeineCacheMetrics.monitor(registry, overrideCache, "overrides");
  }

  // --- Public API (backward-compatible) ---

  /** Global flag evaluation (no identity context). */
  public boolean isEnabled(String flagKey) {
    try {
      boolean result = findFlag(flagKey).map(FeatureFlag::isDefaultEnabled).orElse(false);
      metricsService.incrementCounter(
          "featureflag.evaluation.total",
          "flag",
          flagKey,
          "result",
          String.valueOf(result),
          "scope",
          "global");
      return result;
    } catch (Exception e) {
      boolean fallback = FeatureFlags.defaultEnabled(flagKey);
      metricsService.incrementCounter("featureflag.evaluation.fallback.total", "flag", flagKey);
      log.warn("Failed to evaluate flag '{}', falling back to default: {}", flagKey, fallback, e);
      return fallback;
    }
  }

  public boolean isDisabled(String flagKey) {
    return !isEnabled(flagKey);
  }

  /** Team-level flag evaluation. */
  public boolean isEnabled(String flagKey, UUID teamId) {
    try {
      boolean enabled =
          findFlag(flagKey)
              .map(
                  flag -> {
                    SegmentContext ctx =
                        buildContextForTeam(
                            teamId, false, Optional.empty(), "", false, true, false);
                    List<String> segmentKeys = segmentEvaluator.matchingSegmentKeys(ctx);
                    List<FeatureFlagOverride> overrides =
                        getCachedTeamOverrides(teamId, segmentKeys);
                    List<FeatureFlagOverride> flagOverrides =
                        overrides.stream().filter(o -> o.getFlagKey().equals(flagKey)).toList();
                    return resolve(flag, flagOverrides).enabled();
                  })
              .orElse(false);
      metricsService.incrementCounter(
          "featureflag.evaluation.total",
          "flag",
          flagKey,
          "result",
          String.valueOf(enabled),
          "scope",
          "team");
      return enabled;
    } catch (Exception e) {
      boolean fallback = FeatureFlags.defaultEnabled(flagKey);
      metricsService.incrementCounter("featureflag.evaluation.fallback.total", "flag", flagKey);
      log.warn(
          "Failed to evaluate flag '{}' for team {}, falling back to default: {}",
          flagKey,
          teamId,
          fallback,
          e);
      return fallback;
    }
  }

  /** Identity-aware flag evaluation with user/team traits. */
  public boolean isEnabled(String flagKey, UserPrincipal principal) {
    try {
      boolean result = evaluate(flagKey, principal).enabled();
      metricsService.incrementCounter(
          "featureflag.evaluation.total",
          "flag",
          flagKey,
          "result",
          String.valueOf(result),
          "scope",
          "identity");
      return result;
    } catch (Exception e) {
      boolean fallback = FeatureFlags.defaultEnabled(flagKey);
      metricsService.incrementCounter("featureflag.evaluation.fallback.total", "flag", flagKey);
      log.warn(
          "Failed to evaluate flag '{}' for identity, falling back to default: {}",
          flagKey,
          fallback,
          e);
      return fallback;
    }
  }

  public boolean isDisabled(String flagKey, UserPrincipal principal) {
    return !isEnabled(flagKey, principal);
  }

  /** Get remote config value for a flag (identity-aware). */
  public Optional<Object> getValue(String flagKey, UserPrincipal principal) {
    try {
      return evaluate(flagKey, principal).value().map(v -> (Object) v);
    } catch (Exception e) {
      metricsService.incrementCounter("featureflag.evaluation.fallback.total", "flag", flagKey);
      log.warn("Failed to get value for flag '{}', returning empty", flagKey, e);
      return Optional.empty();
    }
  }

  /** Get all evaluated flags for the current user (for the frontend endpoint). */
  public Map<String, Object> getAllFlags(UserPrincipal principal) {
    try {
      List<FeatureFlag> allFlags = getAllFlagDefs();
      SegmentContext ctx = buildSegmentContext(principal);
      List<String> segmentKeys = segmentEvaluator.matchingSegmentKeys(ctx);
      UUID teamId = principal.requireTeamId();

      List<FeatureFlagOverride> allOverrides =
          getCachedUserOverrides(principal.getUserId(), teamId, segmentKeys);

      Map<String, List<FeatureFlagOverride>> byFlag =
          allOverrides.stream().collect(Collectors.groupingBy(FeatureFlagOverride::getFlagKey));

      Map<String, Object> result = new HashMap<>();
      for (FeatureFlag flag : allFlags) {
        List<FeatureFlagOverride> overrides = byFlag.getOrDefault(flag.getKey(), List.of());
        EvaluatedFlag eval = resolve(flag, overrides);
        Map<String, Object> flagData = new HashMap<>();
        flagData.put("enabled", eval.enabled());
        flagData.put("value", eval.value().orElse(null));
        result.put(flag.getKey(), flagData);
      }
      return result;
    } catch (Exception e) {
      metricsService.incrementCounter("featureflag.evaluation.fallback.total", "flag", "_all");
      log.warn("Failed to get all flags for identity, falling back to compile-time defaults", e);
      return buildDefaultFlagMap();
    }
  }

  /** Get all environment-level flags (no identity context, for backoffice global view). */
  public Map<String, Object> getAllEnvironmentFlags() {
    try {
      Map<String, Object> result = new HashMap<>();
      for (FeatureFlag flag : getAllFlagDefs()) {
        Map<String, Object> flagData = new HashMap<>();
        flagData.put("enabled", flag.isDefaultEnabled());
        flagData.put("value", flag.getDefaultValue().orElse(null));
        result.put(flag.getKey(), flagData);
      }
      return result;
    } catch (Exception e) {
      metricsService.incrementCounter(
          "featureflag.evaluation.fallback.total", "flag", "_all_environment");
      log.warn("Failed to get environment flags, falling back to compile-time defaults", e);
      return buildDefaultFlagMap();
    }
  }

  /**
   * Evaluate all flags for a specific user+team combination with full override resolution
   * (backoffice user inspection).
   */
  public Map<String, Object> evaluateAllForUser(
      UUID userId,
      UUID teamId,
      boolean isOwner,
      TeamRole role,
      String email,
      boolean emailVerified) {
    List<FeatureFlag> allFlags = getAllFlagDefs();
    SegmentContext ctx =
        buildContextForTeam(teamId, isOwner, Optional.of(role), email, emailVerified, false, true);
    List<String> segmentKeys = segmentEvaluator.matchingSegmentKeys(ctx);
    List<FeatureFlagOverride> allOverrides =
        overrideRepo.findApplicableOverrides(userId, teamId, segmentKeys);

    Map<String, List<FeatureFlagOverride>> byFlag =
        allOverrides.stream().collect(Collectors.groupingBy(FeatureFlagOverride::getFlagKey));

    Map<String, Object> result = new HashMap<>();
    for (FeatureFlag flag : allFlags) {
      List<FeatureFlagOverride> overrides = byFlag.getOrDefault(flag.getKey(), List.of());
      EvaluatedFlag eval = resolve(flag, overrides);
      Map<String, Object> flagData = new HashMap<>();
      flagData.put("enabled", eval.enabled());
      flagData.put("value", eval.value().orElse(null));
      result.put(flag.getKey(), flagData);
    }
    return result;
  }

  // --- Internal ---

  private record OverrideCacheKey(Optional<UUID> userId, UUID teamId, List<String> segmentKeys) {}

  private EvaluatedFlag evaluate(String flagKey, UserPrincipal principal) {
    return findFlag(flagKey)
        .map(
            flag -> {
              SegmentContext ctx = buildSegmentContext(principal);
              List<String> segmentKeys = segmentEvaluator.matchingSegmentKeys(ctx);
              UUID teamId = principal.requireTeamId();
              List<FeatureFlagOverride> overrides =
                  getCachedUserOverrides(principal.getUserId(), teamId, segmentKeys);
              List<FeatureFlagOverride> flagOverrides =
                  overrides.stream().filter(o -> o.getFlagKey().equals(flagKey)).toList();
              return resolve(flag, flagOverrides);
            })
        .orElse(EvaluatedFlag.disabled());
  }

  private EvaluatedFlag resolve(FeatureFlag flag, List<FeatureFlagOverride> overrides) {
    return overrides.stream()
        .min(
            Comparator.comparingInt((FeatureFlagOverride o) -> scopePriority(o.getScope()))
                .thenComparingInt(FeatureFlagOverride::getPriority))
        .map(
            o -> {
              metricsService.incrementCounter(
                  "featureflag.override.applied.total",
                  "flag",
                  flag.getKey(),
                  "scope",
                  o.getScope().name());
              return new EvaluatedFlag(o.isEnabled(), o.getValue());
            })
        .orElse(new EvaluatedFlag(flag.isDefaultEnabled(), flag.getDefaultValue()));
  }

  private int scopePriority(OverrideScope scope) {
    return switch (scope) {
      case USER -> 0;
      case TEAM -> 1;
      case SEGMENT -> 2;
    };
  }

  private SegmentContext buildSegmentContext(UserPrincipal principal) {
    return buildContextForTeam(
        principal.requireTeamId(),
        principal.isOwner(),
        principal.getRole(),
        principal.getEmail(),
        principal.isEmailVerified(),
        false,
        true);
  }

  private SegmentContext buildContextForTeam(
      UUID teamId,
      boolean isOwner,
      Optional<TeamRole> role,
      String email,
      boolean emailVerified,
      boolean isTeamScope,
      boolean isUserScope) {
    var team = teamRepository.findById(teamId);
    boolean isDemo = team.map(t -> t.isDemo()).orElse(false);
    long teamAgeDays =
        team.map(t -> ChronoUnit.DAYS.between(t.getCreatedAt(), Instant.now(clock))).orElse(0L);
    Optional<String> teamName = team.map(t -> t.getName());
    Optional<String> teamAdminEmail = teamMemberRepository.findAnyAdminEmailByTeamId(teamId);
    Optional<String> teamOwnerEmail = teamMemberRepository.findOwnerEmailByTeamId(teamId);
    var prefs = teamPreferencesRepository.findByTeamId(teamId);
    return new SegmentContext(
        isDemo,
        isOwner,
        role,
        email.isEmpty() ? Optional.empty() : Optional.of(email),
        emailVerified,
        propertyRepository.countByTeamId(teamId),
        teamMemberRepository.countByTeamId(teamId),
        teamAgeDays,
        contractRepository.countByTeamId(teamId),
        contactRepository.countByTeamId(teamId),
        photoRepository.countByTeamId(teamId),
        documentRepository.countByTeamId(teamId),
        expenseRepository.countByTeamId(teamId),
        paymentRepository.countByTeamId(teamId),
        calendarFeedRepository.countByTeamId(teamId),
        isTeamScope,
        isUserScope,
        teamName,
        teamAdminEmail,
        teamOwnerEmail,
        prefs.map(p -> p.getDefaultCurrency()),
        prefs.map(p -> p.getDefaultCountryCode()),
        prefs.map(p -> p.getTimezone()));
  }

  private Optional<FeatureFlag> findFlag(String flagKey) {
    return getAllFlagDefs().stream().filter(f -> f.getKey().equals(flagKey)).findFirst();
  }

  List<FeatureFlag> getAllFlagDefs() {
    return flagDefsCache.get("all");
  }

  private List<FeatureFlagOverride> getCachedUserOverrides(
      UUID userId, UUID teamId, List<String> segmentKeys) {
    OverrideCacheKey key = new OverrideCacheKey(Optional.of(userId), teamId, segmentKeys);
    return overrideCache.get(
        key, k -> overrideRepo.findApplicableOverrides(userId, teamId, segmentKeys));
  }

  private List<FeatureFlagOverride> getCachedTeamOverrides(UUID teamId, List<String> segmentKeys) {
    OverrideCacheKey key = new OverrideCacheKey(Optional.empty(), teamId, segmentKeys);
    return overrideCache.get(key, k -> overrideRepo.findTeamOverrides(teamId, segmentKeys));
  }

  private Map<String, Object> buildDefaultFlagMap() {
    Map<String, Object> result = new HashMap<>();
    for (var entry : FeatureFlags.DEFAULTS.entrySet()) {
      Map<String, Object> flagData = new HashMap<>();
      flagData.put("enabled", entry.getValue());
      flagData.put("value", null);
      result.put(entry.getKey(), flagData);
    }
    return result;
  }

  /** Invalidate all caches (called by admin service after mutations). */
  public void invalidateCaches() {
    flagDefsCache.invalidateAll();
    overrideCache.invalidateAll();
    lastInvalidatedAt = Optional.of(Instant.now(clock));
  }

  /** Cache stats for backoffice inspection. */
  public Map<String, Object> getFlagDefsCacheInfo() {
    var stats = flagDefsCache.stats();
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("name", "flag_definitions");
    info.put("type", "LoadingCache");
    info.put("description", "All active feature flag definitions");
    info.put("maxSize", 1);
    info.put("ttlSeconds", 60);
    info.put("refreshPolicy", "refresh-after-write");
    info.put("entryCount", flagDefsCache.estimatedSize());
    info.put("hitCount", stats.hitCount());
    info.put("missCount", stats.missCount());
    info.put("hitRate", stats.hitCount() + stats.missCount() > 0 ? stats.hitRate() : 0.0);
    info.put("loadCount", stats.loadCount());
    info.put("averageLoadTimeMs", stats.averageLoadPenalty() / 1_000_000.0);
    info.put("evictionCount", stats.evictionCount());
    info.put("lastInvalidatedAt", lastInvalidatedAt.orElse(null));
    return info;
  }

  public Map<String, Object> getOverrideCacheInfo() {
    var stats = overrideCache.stats();
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("name", "overrides");
    info.put("type", "Cache");
    info.put("description", "Team/user/segment override lookups");
    info.put("maxSize", 1000);
    info.put("ttlSeconds", 30);
    info.put("refreshPolicy", "expire-after-write");
    info.put("entryCount", overrideCache.estimatedSize());
    info.put("hitCount", stats.hitCount());
    info.put("missCount", stats.missCount());
    info.put("hitRate", stats.hitCount() + stats.missCount() > 0 ? stats.hitRate() : 0.0);
    info.put("loadCount", stats.loadCount());
    info.put("averageLoadTimeMs", stats.averageLoadPenalty() / 1_000_000.0);
    info.put("evictionCount", stats.evictionCount());
    info.put("lastInvalidatedAt", lastInvalidatedAt.orElse(null));
    return info;
  }

  /** Cache entries for backoffice inspection. */
  public List<Map<String, Object>> getFlagDefsCacheEntries() {
    List<Map<String, Object>> entries = new ArrayList<>();
    flagDefsCache
        .asMap()
        .forEach(
            (key, flags) -> {
              Map<String, Object> entry = new LinkedHashMap<>();
              entry.put("key", key);
              entry.put("summary", flags.size() + " flag definitions");
              entry.put(
                  "details",
                  flags.stream()
                      .map(
                          f -> {
                            Map<String, Object> detail = new LinkedHashMap<>();
                            detail.put("key", f.getKey());
                            detail.put("enabled", f.isDefaultEnabled());
                            detail.put("value", f.getDefaultValue().orElse(null));
                            detail.put("valueType", f.getValueType());
                            return detail;
                          })
                      .toList());
              entries.add(entry);
            });
    return entries;
  }

  public List<Map<String, Object>> getOverrideCacheEntries() {
    List<Map<String, Object>> entries = new ArrayList<>();
    overrideCache
        .asMap()
        .forEach(
            (key, overrides) -> {
              Map<String, Object> entry = new LinkedHashMap<>();
              entry.put("key", formatOverrideCacheKey(key));
              entry.put("summary", overrides.size() + " overrides");
              entry.put(
                  "details",
                  overrides.stream()
                      .map(
                          o -> {
                            Map<String, Object> detail = new LinkedHashMap<>();
                            detail.put("flagKey", o.getFlagKey());
                            detail.put("scope", o.getScope().name());
                            detail.put("enabled", o.isEnabled());
                            detail.put("value", o.getValue().orElse(null));
                            return detail;
                          })
                      .toList());
              entries.add(entry);
            });
    return entries;
  }

  private String formatOverrideCacheKey(OverrideCacheKey key) {
    StringBuilder sb = new StringBuilder();
    key.userId().ifPresent(uid -> sb.append("user=").append(uid).append(", "));
    sb.append("team=").append(key.teamId());
    if (!key.segmentKeys().isEmpty()) {
      sb.append(", segments=").append(key.segmentKeys());
    }
    return sb.toString();
  }
}
