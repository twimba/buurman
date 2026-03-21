package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.JSONB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.NotificationType;
import com.buurman.domain.NotificationUrgency;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.NotificationsRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationRecordMapper")
class NotificationRecordMapperTest {

  @Mock private ObjectMapper objectMapper;
  @InjectMocks private NotificationRecordMapper mapper;

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID RECIPIENT_USER_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("NTF01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<Notification> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      NotificationsRecord record = createCompleteRecord();

      Optional<Notification> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      Notification notification = result.get();
      assertThat(notification.getId()).isEqualTo(ID);
      assertThat(notification.getIdentifier()).contains(IDENTIFIER);
      assertThat(notification.getTeamId()).contains(TEAM_ID);
      assertThat(notification.getNotificationType()).isEqualTo(NotificationType.PAYMENT_REMINDER);
      assertThat(notification.getSubject()).contains("Payment due");
      assertThat(notification.getBody()).isEqualTo("Your payment is due");
      assertThat(notification.getRecipientEmail()).contains("tenant@example.com");
      assertThat(notification.getRecipientUserId()).contains(RECIPIENT_USER_ID);
      assertThat(notification.getChannel()).isEqualTo(NotificationChannel.EMAIL);
      assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
      assertThat(notification.getOpenCount()).isEqualTo(2);
      assertThat(notification.getClickCount()).isEqualTo(1);
      assertThat(notification.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(notification.getCreatedBy()).contains(CREATED_BY);
    }

    @Test
    @DisplayName("maps all NotificationType enum values correctly")
    void mapsAllNotificationTypes() {
      for (NotificationType type : NotificationType.values()) {
        NotificationsRecord record = createCompleteRecord();
        record.setNotificationType(type.name());

        Optional<Notification> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getNotificationType()).isEqualTo(type);
      }
    }

    @Test
    @DisplayName("maps all NotificationStatus enum values correctly")
    void mapsAllNotificationStatuses() {
      for (NotificationStatus status : NotificationStatus.values()) {
        NotificationsRecord record = createCompleteRecord();
        record.setStatus(status.name());

        Optional<Notification> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("maps both NotificationChannel values correctly")
    void mapsNotificationChannels() {
      for (NotificationChannel channel : NotificationChannel.values()) {
        NotificationsRecord record = createCompleteRecord();
        record.setChannel(channel.name());

        Optional<Notification> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getChannel()).isEqualTo(channel);
      }
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      NotificationsRecord record = createCompleteRecord();
      record.setTeamId(null);
      record.setSubject(null);
      record.setRecipientEmail(null);
      record.setRecipientPhone(null);
      record.setRecipientUserId(null);
      record.setRecipientTenantId(null);
      record.setContentTemplate(null);
      record.setProviderMessageId(null);
      record.setProviderStatus(null);
      record.setProviderError(null);
      record.setStatusUpdatedAt(null);
      record.setFirstOpenedAt(null);
      record.setFirstClickedAt(null);
      record.setResentFromId(null);
      record.setResendReason(null);
      record.setCreatedBy(null);

      Optional<Notification> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      Notification notification = result.get();
      assertThat(notification.getTeamId()).isEmpty();
      assertThat(notification.getSubject()).isEmpty();
      assertThat(notification.getRecipientEmail()).isEmpty();
      assertThat(notification.getRecipientPhone()).isEmpty();
      assertThat(notification.getRecipientUserId()).isEmpty();
      assertThat(notification.getRecipientTenantId()).isEmpty();
      assertThat(notification.getContentTemplate()).isEmpty();
      assertThat(notification.getProviderMessageId()).isEmpty();
      assertThat(notification.getProviderStatus()).isEmpty();
      assertThat(notification.getProviderError()).isEmpty();
      assertThat(notification.getStatusUpdatedAt()).isEmpty();
      assertThat(notification.getFirstOpenedAt()).isEmpty();
      assertThat(notification.getFirstClickedAt()).isEmpty();
      assertThat(notification.getResentFromId()).isEmpty();
      assertThat(notification.getResendReason()).isEmpty();
      assertThat(notification.getCreatedBy()).isEmpty();
    }

    @Test
    @DisplayName("maps timestamp fields to Instant at UTC")
    void mapsTimestampFields() {
      NotificationsRecord record = createCompleteRecord();
      LocalDateTime statusUpdated = LocalDateTime.of(2026, 3, 2, 10, 0, 0);
      LocalDateTime firstOpened = LocalDateTime.of(2026, 3, 2, 11, 30, 0);
      LocalDateTime firstClicked = LocalDateTime.of(2026, 3, 2, 12, 0, 0);
      record.setStatusUpdatedAt(statusUpdated);
      record.setFirstOpenedAt(firstOpened);
      record.setFirstClickedAt(firstClicked);

      Optional<Notification> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      Notification notification = result.get();
      assertThat(notification.getStatusUpdatedAt())
          .contains(statusUpdated.toInstant(ZoneOffset.UTC));
      assertThat(notification.getFirstOpenedAt())
          .contains(firstOpened.toInstant(ZoneOffset.UTC));
      assertThat(notification.getFirstClickedAt())
          .contains(firstClicked.toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("defaults urgency to NORMAL when urgency field is null")
    void defaultsUrgencyToNormal() {
      // NotificationsRecord doesn't have a typed urgency column — it's read via
      // record.get("urgency", String.class). On a plain record this returns null,
      // and the mapper defaults to NotificationUrgency.NORMAL.
      NotificationsRecord record = createCompleteRecord();

      Optional<Notification> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getUrgency()).isEqualTo(NotificationUrgency.NORMAL);
    }
  }

  @Nested
  @DisplayName("content variables JSONB deserialization")
  class ContentVariablesDeserialization {

    @Test
    @DisplayName("deserializes valid JSONB content variables")
    void deserializesValidJsonb() throws Exception {
      NotificationsRecord record = createCompleteRecord();
      String json = "{\"amount\":\"1200.00\",\"currency\":\"EUR\"}";
      record.setContentVariables(JSONB.jsonb(json));

      Map<String, Object> expected = Map.of("amount", "1200.00", "currency", "EUR");
      when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(expected);

      Optional<Notification> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getContentVariables()).isPresent();
      assertThat(result.get().getContentVariables().get())
          .containsEntry("amount", "1200.00")
          .containsEntry("currency", "EUR");
    }

    @Test
    @DisplayName("does not set content variables when JSONB is null")
    void doesNotSetWhenJsonbNull() {
      NotificationsRecord record = createCompleteRecord();
      record.setContentVariables(null);

      Optional<Notification> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      // contentVariables is never explicitly set, so it stays at field default (Optional.empty)
      // from the Notification @Builder.Default
      assertThat(result.get().getContentVariables()).isEmpty();
    }

    @Test
    @DisplayName("gracefully handles deserialization failure without throwing")
    void handlesDeserializationFailureGracefully() throws Exception {
      NotificationsRecord record = createCompleteRecord();
      record.setContentVariables(JSONB.jsonb("invalid json"));

      when(objectMapper.readValue(eq("invalid json"), any(TypeReference.class)))
          .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "parse error"));

      Optional<Notification> result = mapper.toDomain(record);

      // Should not throw — mapper catches the exception and continues.
      assertThat(result).isPresent();
      // contentVariables is never set on failure, stays at default Optional.empty()
      assertThat(result.get().getContentVariables()).isEmpty();
    }

    @Test
    @DisplayName("deserializes empty JSON object to empty map")
    void deserializesEmptyJsonObject() throws Exception {
      NotificationsRecord record = createCompleteRecord();
      record.setContentVariables(JSONB.jsonb("{}"));

      when(objectMapper.readValue(eq("{}"), any(TypeReference.class)))
          .thenReturn(Map.of());

      Optional<Notification> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getContentVariables()).isPresent();
      assertThat(result.get().getContentVariables().get()).isEmpty();
    }
  }

  private NotificationsRecord createCompleteRecord() {
    NotificationsRecord record = new NotificationsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setTeamId(TEAM_ID);
    record.setNotificationType("PAYMENT_REMINDER");
    record.setSubject("Payment due");
    record.setBody("Your payment is due");
    record.setRecipientEmail("tenant@example.com");
    record.setRecipientPhone(null);
    record.setRecipientUserId(RECIPIENT_USER_ID);
    record.setRecipientTenantId(null);
    record.setChannel("EMAIL");
    record.setContentTemplate("payment-reminder");
    record.setContentVariables(null);
    record.setStatus("SENT");
    record.setProviderMessageId("msg-123");
    record.setProviderStatus("delivered");
    record.setProviderError(null);
    record.setStatusUpdatedAt(null);
    record.setOpenCount(2);
    record.setClickCount(1);
    record.setFirstOpenedAt(null);
    record.setFirstClickedAt(null);
    record.setResentFromId(null);
    record.setResendReason(null);
    record.setCreatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    return record;
  }
}
