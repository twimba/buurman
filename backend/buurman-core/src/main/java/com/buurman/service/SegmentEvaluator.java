package com.buurman.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.buurman.domain.SegmentAttribute;
import com.buurman.domain.SegmentCondition;
import com.buurman.domain.SegmentContext;
import com.buurman.domain.SegmentDefinition;
import com.buurman.repository.SegmentRepository;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SegmentEvaluator {

  private final SegmentRepository segmentRepository;
  private final MetricsService metricsService;
  private final MeterRegistry registry;
  private final Clock clock;
  private final LoadingCache<String, List<SegmentDefinition>> segmentsCache;
  private Optional<Instant> lastInvalidatedAt = Optional.empty();

  public SegmentEvaluator(
      SegmentRepository segmentRepository,
      MetricsService metricsService,
      MeterRegistry registry,
      Clock clock) {
    this.segmentRepository = segmentRepository;
    this.metricsService = metricsService;
    this.registry = registry;
    this.clock = clock;
    this.segmentsCache =
        Caffeine.newBuilder()
            .maximumSize(1)
            .refreshAfterWrite(60, TimeUnit.SECONDS)
            .recordStats()
            .build(key -> segmentRepository.findAll());
    CaffeineCacheMetrics.monitor(registry, segmentsCache, "segments");
  }

  public List<String> matchingSegmentKeys(SegmentContext ctx) {
    List<String> matchedKeys =
        getAllSegments().stream()
            .filter(segment -> matchesAll(segment.getConditions(), ctx))
            .map(SegmentDefinition::getKey)
            .toList();
    matchedKeys.forEach(
        key -> metricsService.incrementCounter("featureflag.segment.match.total", "segment", key));
    return matchedKeys;
  }

  List<SegmentDefinition> getAllSegments() {
    return segmentsCache.get("all");
  }

  public void invalidateCache() {
    segmentsCache.invalidateAll();
    lastInvalidatedAt = Optional.of(Instant.now(clock));
  }

  public Map<String, Object> getCacheInfo() {
    var stats = segmentsCache.stats();
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("name", "segments");
    info.put("type", "LoadingCache");
    info.put("description", "All segment definitions for flag targeting");
    info.put("maxSize", 1);
    info.put("ttlSeconds", 60);
    info.put("refreshPolicy", "refresh-after-write");
    info.put("entryCount", segmentsCache.estimatedSize());
    info.put("hitCount", stats.hitCount());
    info.put("missCount", stats.missCount());
    info.put("hitRate", stats.hitCount() + stats.missCount() > 0 ? stats.hitRate() : 0.0);
    info.put("loadCount", stats.loadCount());
    info.put("averageLoadTimeMs", stats.averageLoadPenalty() / 1_000_000.0);
    info.put("evictionCount", stats.evictionCount());
    info.put("lastInvalidatedAt", lastInvalidatedAt.orElse(null));
    return info;
  }

  public List<Map<String, Object>> getCacheEntries() {
    List<Map<String, Object>> entries = new ArrayList<>();
    segmentsCache
        .asMap()
        .forEach(
            (key, segments) -> {
              Map<String, Object> entry = new LinkedHashMap<>();
              entry.put("key", key);
              entry.put("summary", segments.size() + " segment definitions");
              entry.put(
                  "details",
                  segments.stream()
                      .map(
                          s -> {
                            Map<String, Object> detail = new LinkedHashMap<>();
                            detail.put("key", s.getKey());
                            detail.put("name", s.getName());
                            detail.put("priority", s.getPriority());
                            detail.put("conditionCount", s.getConditions().size());
                            return detail;
                          })
                      .toList());
              entries.add(entry);
            });
    return entries;
  }

  private boolean matchesAll(List<SegmentCondition> conditions, SegmentContext ctx) {
    if (conditions.isEmpty()) {
      return false;
    }
    return conditions.stream().allMatch(c -> evaluateCondition(c, ctx));
  }

  private boolean evaluateCondition(SegmentCondition condition, SegmentContext ctx) {
    String actual = resolveAttribute(condition.getAttribute(), ctx);
    String expected = condition.getValue();

    return switch (condition.getOperator()) {
      case EQ -> expected.equals(actual);
      case NEQ -> !expected.equals(actual);
      case IN -> Set.of(expected.split(",")).contains(actual);
      case NOT_IN -> !Set.of(expected.split(",")).contains(actual);
      case GT -> parseLong(actual) > parseLong(expected);
      case GTE -> parseLong(actual) >= parseLong(expected);
      case LT -> parseLong(actual) < parseLong(expected);
      case LTE -> parseLong(actual) <= parseLong(expected);
      case CONTAINS -> actual.contains(expected);
      case NOT_CONTAINS -> !actual.contains(expected);
      case STARTS_WITH -> actual.startsWith(expected);
      case ENDS_WITH -> actual.endsWith(expected);
      case REGEX -> {
        try {
          yield actual.matches(expected);
        } catch (java.util.regex.PatternSyntaxException e) {
          yield false;
        }
      }
    };
  }

  private String resolveAttribute(SegmentAttribute attr, SegmentContext ctx) {
    return switch (attr) {
      case IS_DEMO -> String.valueOf(ctx.isDemo());
      case IS_OWNER -> String.valueOf(ctx.isOwner());
      case ROLE -> ctx.role().map(Enum::name).orElse("");
      case EMAIL -> ctx.email().orElse("");
      case EMAIL_VERIFIED -> String.valueOf(ctx.emailVerified());
      case PROPERTY_COUNT -> String.valueOf(ctx.propertyCount());
      case MEMBER_COUNT -> String.valueOf(ctx.memberCount());
      case TEAM_AGE_DAYS -> String.valueOf(ctx.teamAgeDays());
      case CONTRACT_COUNT -> String.valueOf(ctx.contractCount());
      case CONTACT_COUNT -> String.valueOf(ctx.contactCount());
      case PHOTO_COUNT -> String.valueOf(ctx.photoCount());
      case DOCUMENT_COUNT -> String.valueOf(ctx.documentCount());
      case EXPENSE_COUNT -> String.valueOf(ctx.expenseCount());
      case PAYMENT_COUNT -> String.valueOf(ctx.paymentCount());
      case CALENDAR_FEED_COUNT -> String.valueOf(ctx.calendarFeedCount());
      case IS_TEAM_SCOPE -> String.valueOf(ctx.isTeamScope());
      case IS_USER_SCOPE -> String.valueOf(ctx.isUserScope());
    };
  }

  private long parseLong(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return 0L;
    }
  }
}
