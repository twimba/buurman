package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService")
class AuditServiceTest {

  @Mock private AuditLogRepository auditLogRepository;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();
  @Mock private MetricsService metricsService;

  private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  // Construct manually because Clock is final and @InjectMocks won't inject the fixed clock.
  private AuditService createService() {
    return new AuditService(auditLogRepository, objectMapper, metricsService, clock);
  }

  @Nested
  @DisplayName("getChangedFields")
  class GetChangedFields {

    @Test
    @DisplayName("detects changed string field")
    void detectsChangedStringField() {
      AuditService svc = createService();
      Map<String, Object> old = Map.of("name", "Alice", "email", "alice@example.com");
      Map<String, Object> updated = Map.of("name", "Bob", "email", "alice@example.com");

      Map<String, Object> changes = svc.getChangedFields(old, updated);

      assertThat(changes).containsEntry("name", "Bob");
      assertThat(changes).doesNotContainKey("email");
    }

    @Test
    @DisplayName("detects new field added")
    void detectsNewFieldAdded() {
      AuditService svc = createService();
      Map<String, Object> old = Map.of("name", "Alice");
      Map<String, Object> updated = Map.of("name", "Alice", "phone", "123");

      Map<String, Object> changes = svc.getChangedFields(old, updated);

      assertThat(changes).containsEntry("phone", "123");
      assertThat(changes).doesNotContainKey("name");
    }

    @Test
    @DisplayName("returns empty map when no changes")
    void returnsEmptyMapWhenNoChanges() {
      AuditService svc = createService();
      Map<String, Object> data = Map.of("name", "Alice", "email", "alice@example.com");

      Map<String, Object> changes = svc.getChangedFields(data, data);

      assertThat(changes).isEmpty();
    }

    @Test
    @DisplayName("handles BigDecimal comparison correctly")
    void handlesBigDecimalComparison() {
      AuditService svc = createService();
      // BigDecimal("1.0") and BigDecimal("1.00") are not equal via equals() but
      // should be considered equal via compareTo()
      Map<String, Object> old = Map.of("amount", new BigDecimal("1.0"));
      Map<String, Object> updated = Map.of("amount", new BigDecimal("1.00"));

      Map<String, Object> changes = svc.getChangedFields(old, updated);

      assertThat(changes).isEmpty();
    }

    @Test
    @DisplayName("detects changed BigDecimal value")
    void detectsChangedBigDecimal() {
      AuditService svc = createService();
      Map<String, Object> old = Map.of("amount", new BigDecimal("100.00"));
      Map<String, Object> updated = Map.of("amount", new BigDecimal("200.00"));

      Map<String, Object> changes = svc.getChangedFields(old, updated);

      assertThat(changes).containsEntry("amount", new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("detects field changed from null to value")
    void detectsFieldChangedFromNullToValue() {
      AuditService svc = createService();
      // Use HashMap to allow null values
      java.util.Map<String, Object> old = new java.util.HashMap<>();
      old.put("name", "Alice");
      old.put("phone", null);
      Map<String, Object> updated = Map.of("name", "Alice", "phone", "123");

      Map<String, Object> changes = svc.getChangedFields(old, updated);

      assertThat(changes).containsEntry("phone", "123");
    }

    @Test
    @DisplayName("handles domain objects via ObjectMapper conversion")
    void handlesDomainObjects() {
      AuditService svc = createService();
      record TestEntity(String name, int age) {}
      TestEntity old = new TestEntity("Alice", 30);
      TestEntity updated = new TestEntity("Alice", 31);

      Map<String, Object> changes = svc.getChangedFields(old, updated);

      assertThat(changes).containsEntry("age", 31);
      assertThat(changes).doesNotContainKey("name");
    }

    @Test
    @DisplayName("detects field changed from non-null to null")
    void detectsFieldChangedFromNonNullToNull() {
      AuditService svc = createService();
      Map<String, Object> old = Map.of("name", "Alice", "phone", "123");
      // New entity has phone set to null — use HashMap to allow null values
      java.util.HashMap<String, Object> updated = new java.util.HashMap<>();
      updated.put("name", "Alice");
      updated.put("phone", null);

      Map<String, Object> changes = svc.getChangedFields(old, updated);

      // The new value is null but the old value was "123", so it should be detected
      assertThat(changes).containsKey("phone");
      assertThat(changes.get("phone")).isNull();
    }

    @Test
    @DisplayName("silently drops field present in old but absent in new")
    void silentlyDropsFieldAbsentInNew() {
      AuditService svc = createService();
      // Old entity has a "phone" field, new entity does not have it at all
      Map<String, Object> old = Map.of("name", "Alice", "phone", "123");
      Map<String, Object> updated = Map.of("name", "Alice");

      Map<String, Object> changes = svc.getChangedFields(old, updated);

      // getChangedFields iterates newMap.keySet(), so "phone" is never visited
      assertThat(changes).doesNotContainKey("phone");
      assertThat(changes).isEmpty();
    }
  }
}
