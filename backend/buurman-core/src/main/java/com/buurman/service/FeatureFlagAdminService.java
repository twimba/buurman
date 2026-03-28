package com.buurman.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.FeatureFlag;
import com.buurman.domain.FeatureFlagOverride;
import com.buurman.domain.OverrideScope;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.FeatureFlagOverrideRepository;
import com.buurman.repository.FeatureFlagRepository;
import com.buurman.repository.SegmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureFlagAdminService {

  private final FeatureFlagRepository flagRepo;
  private final FeatureFlagOverrideRepository overrideRepo;
  private final FeatureFlagService featureFlagService;
  private final SegmentRepository segmentRepository;
  private final MetricsService metricsService;

  public List<FeatureFlag> listFlags() {
    return flagRepo.findAll();
  }

  public FeatureFlag getFlag(String key) {
    return flagRepo
        .findByKey(key)
        .orElseThrow(() -> new NotFoundException("Feature flag '%s' not found".formatted(key)));
  }

  @Transactional
  public FeatureFlag createFlag(
      String key,
      String valueType,
      boolean defaultEnabled,
      Optional<String> defaultValue,
      Optional<String> description,
      UUID actorId) {
    FeatureFlag flag =
        FeatureFlag.builder()
            .key(key)
            .valueType(valueType)
            .defaultEnabled(defaultEnabled)
            .defaultValue(defaultValue)
            .description(description)
            .build();
    FeatureFlag saved = flagRepo.save(flag, actorId);
    featureFlagService.invalidateCaches();
    metricsService.incrementCounter(
        "featureflag.admin.mutation.total", "operation", "create", "target", "flag");
    return saved;
  }

  @Transactional
  public FeatureFlag updateFlag(
      String key,
      Optional<Boolean> enabled,
      Optional<String> value,
      Optional<String> description,
      UUID actorId) {
    FeatureFlag flag = getFlag(key);
    enabled.ifPresent(flag::setDefaultEnabled);
    value.ifPresent(v -> flag.setDefaultValue(Optional.ofNullable(v)));
    description.ifPresent(d -> flag.setDescription(Optional.ofNullable(d)));
    FeatureFlag saved = flagRepo.save(flag, actorId);
    featureFlagService.invalidateCaches();
    metricsService.incrementCounter(
        "featureflag.admin.mutation.total", "operation", "update", "target", "flag");
    return saved;
  }

  @Transactional
  public void deleteFlag(String key, UUID actorId) {
    flagRepo.softDelete(key, actorId);
    featureFlagService.invalidateCaches();
    metricsService.incrementCounter(
        "featureflag.admin.mutation.total", "operation", "delete", "target", "flag");
  }

  public List<FeatureFlagOverride> listOverrides(String flagKey) {
    return overrideRepo.findByFlagKey(flagKey);
  }

  @Transactional
  public FeatureFlagOverride upsertTeamOverride(
      String flagKey, UUID teamId, boolean enabled, Optional<String> value, UUID actorId) {
    Optional<FeatureFlagOverride> existing =
        overrideRepo.findByScope(
            flagKey, OverrideScope.TEAM, Optional.of(teamId), Optional.empty(), Optional.empty());

    FeatureFlagOverride override =
        existing.orElseGet(
            () ->
                FeatureFlagOverride.builder()
                    .flagKey(flagKey)
                    .scope(OverrideScope.TEAM)
                    .teamId(Optional.of(teamId))
                    .build());

    override.setEnabled(enabled);
    override.setValue(value);
    FeatureFlagOverride saved = overrideRepo.save(override, actorId);
    featureFlagService.invalidateCaches();
    metricsService.incrementCounter(
        "featureflag.admin.mutation.total", "operation", "upsert", "target", "team_override");
    return saved;
  }

  @Transactional
  public FeatureFlagOverride upsertUserOverride(
      String flagKey,
      UUID teamId,
      UUID userId,
      boolean enabled,
      Optional<String> value,
      UUID actorId) {
    Optional<FeatureFlagOverride> existing =
        overrideRepo.findByScope(
            flagKey,
            OverrideScope.USER,
            Optional.of(teamId),
            Optional.of(userId),
            Optional.empty());

    FeatureFlagOverride override =
        existing.orElseGet(
            () ->
                FeatureFlagOverride.builder()
                    .flagKey(flagKey)
                    .scope(OverrideScope.USER)
                    .teamId(Optional.of(teamId))
                    .userId(Optional.of(userId))
                    .build());

    override.setEnabled(enabled);
    override.setValue(value);
    FeatureFlagOverride saved = overrideRepo.save(override, actorId);
    featureFlagService.invalidateCaches();
    metricsService.incrementCounter(
        "featureflag.admin.mutation.total", "operation", "upsert", "target", "user_override");
    return saved;
  }

  @Transactional
  public FeatureFlagOverride upsertSegmentOverride(
      String flagKey,
      String segmentKey,
      boolean enabled,
      Optional<String> value,
      int priority,
      UUID actorId) {
    Optional<FeatureFlagOverride> existing =
        overrideRepo.findByScope(
            flagKey,
            OverrideScope.SEGMENT,
            Optional.empty(),
            Optional.empty(),
            Optional.of(segmentKey));

    FeatureFlagOverride override =
        existing.orElseGet(
            () ->
                FeatureFlagOverride.builder()
                    .flagKey(flagKey)
                    .scope(OverrideScope.SEGMENT)
                    .segmentKey(Optional.of(segmentKey))
                    .build());

    override.setEnabled(enabled);
    override.setValue(value);
    override.setPriority(priority);
    FeatureFlagOverride saved = overrideRepo.save(override, actorId);
    featureFlagService.invalidateCaches();
    metricsService.incrementCounter(
        "featureflag.admin.mutation.total", "operation", "upsert", "target", "segment_override");
    return saved;
  }

  @Transactional
  public void deleteTeamOverride(String flagKey, UUID teamId, UUID actorId) {
    overrideRepo
        .findByScope(
            flagKey, OverrideScope.TEAM, Optional.of(teamId), Optional.empty(), Optional.empty())
        .ifPresent(o -> overrideRepo.softDelete(o.getId(), actorId));
    featureFlagService.invalidateCaches();
    metricsService.incrementCounter(
        "featureflag.admin.mutation.total", "operation", "delete", "target", "team_override");
  }

  @Transactional
  public void deleteUserOverride(String flagKey, UUID teamId, UUID userId, UUID actorId) {
    overrideRepo
        .findByScope(
            flagKey, OverrideScope.USER, Optional.of(teamId), Optional.of(userId), Optional.empty())
        .ifPresent(o -> overrideRepo.softDelete(o.getId(), actorId));
    featureFlagService.invalidateCaches();
    metricsService.incrementCounter(
        "featureflag.admin.mutation.total", "operation", "delete", "target", "user_override");
  }

  @Transactional
  public void deleteSegmentOverride(String flagKey, String segmentKey, UUID actorId) {
    overrideRepo
        .findByScope(
            flagKey,
            OverrideScope.SEGMENT,
            Optional.empty(),
            Optional.empty(),
            Optional.of(segmentKey))
        .ifPresent(o -> overrideRepo.softDelete(o.getId(), actorId));
    featureFlagService.invalidateCaches();
    metricsService.incrementCounter(
        "featureflag.admin.mutation.total", "operation", "delete", "target", "segment_override");
  }

  public List<Map<String, Object>> listSegments() {
    List<FeatureFlagOverride> allSegmentOverrides = overrideRepo.findAllSegmentOverrides();
    Map<String, List<FeatureFlagOverride>> overridesBySegment =
        allSegmentOverrides.stream()
            .collect(Collectors.groupingBy(o -> o.getSegmentKey().orElse("")));

    return segmentRepository.findAll().stream()
        .map(
            segment -> {
              Map<String, Object> segmentData = new java.util.HashMap<>();
              segmentData.put("segmentKey", segment.getKey());
              segmentData.put("segmentName", segment.getName());
              segmentData.put("description", segment.getDescription().orElse(""));

              List<FeatureFlagOverride> segOverrides =
                  overridesBySegment.getOrDefault(segment.getKey(), List.of());
              Map<String, Object> overrides = new java.util.HashMap<>();
              for (FeatureFlagOverride o : segOverrides) {
                Map<String, Object> flagOverride = new java.util.HashMap<>();
                flagOverride.put("flagName", o.getFlagKey());
                flagOverride.put("enabled", o.isEnabled());
                flagOverride.put("value", o.getValue().orElse(null));
                overrides.put(o.getFlagKey(), flagOverride);
              }
              segmentData.put("overrides", overrides);
              return segmentData;
            })
        .toList();
  }
}
