package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.USER_NOTIFICATION_TYPE_PREFERENCES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.NotificationType;
import com.buurman.domain.UserNotificationTypePreference;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserNotificationTypePreferenceRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<UserNotificationTypePreference> findByUserId(UUID userId) {
    return dsl.selectFrom(USER_NOTIFICATION_TYPE_PREFERENCES)
        .where(USER_NOTIFICATION_TYPE_PREFERENCES.USER_ID.eq(userId))
        .fetch()
        .map(this::toDomain);
  }

  public Optional<UserNotificationTypePreference> findByUserIdAndType(
      UUID userId, NotificationType type) {
    return dsl.selectFrom(USER_NOTIFICATION_TYPE_PREFERENCES)
        .where(USER_NOTIFICATION_TYPE_PREFERENCES.USER_ID.eq(userId))
        .and(USER_NOTIFICATION_TYPE_PREFERENCES.NOTIFICATION_TYPE.eq(type.name()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public void saveAll(UUID userId, List<UserNotificationTypePreference> prefs) {
    LocalDateTime now = LocalDateTime.now(clock);

    for (UserNotificationTypePreference pref : prefs) {
      LocalDateTime createdAt =
          pref.getCreatedAt() != null ? LocalDateTime.ofInstant(pref.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          pref.getUpdatedAt() != null ? LocalDateTime.ofInstant(pref.getUpdatedAt(), UTC) : now;

      dsl.insertInto(USER_NOTIFICATION_TYPE_PREFERENCES)
          .set(USER_NOTIFICATION_TYPE_PREFERENCES.ID, UUID.randomUUID())
          .set(USER_NOTIFICATION_TYPE_PREFERENCES.USER_ID, userId)
          .set(
              USER_NOTIFICATION_TYPE_PREFERENCES.NOTIFICATION_TYPE,
              pref.getNotificationType().name())
          .set(USER_NOTIFICATION_TYPE_PREFERENCES.EMAIL_ENABLED, pref.isEmailEnabled())
          .set(USER_NOTIFICATION_TYPE_PREFERENCES.SMS_ENABLED, pref.isSmsEnabled())
          .set(USER_NOTIFICATION_TYPE_PREFERENCES.CREATED_AT, createdAt)
          .set(USER_NOTIFICATION_TYPE_PREFERENCES.UPDATED_AT, updatedAt)
          .onConflict(
              USER_NOTIFICATION_TYPE_PREFERENCES.USER_ID,
              USER_NOTIFICATION_TYPE_PREFERENCES.NOTIFICATION_TYPE)
          .doUpdate()
          .set(USER_NOTIFICATION_TYPE_PREFERENCES.EMAIL_ENABLED, pref.isEmailEnabled())
          .set(USER_NOTIFICATION_TYPE_PREFERENCES.SMS_ENABLED, pref.isSmsEnabled())
          .set(USER_NOTIFICATION_TYPE_PREFERENCES.UPDATED_AT, updatedAt)
          .execute();
    }
  }

  private UserNotificationTypePreference toDomain(
      com.buurman.jooq.generated.tables.records.UserNotificationTypePreferencesRecord record) {
    UserNotificationTypePreference pref = new UserNotificationTypePreference();
    pref.setId(record.getId());
    pref.setUserId(record.getUserId());
    pref.setNotificationType(NotificationType.valueOf(record.getNotificationType()));
    pref.setEmailEnabled(record.getEmailEnabled());
    pref.setSmsEnabled(record.getSmsEnabled());
    pref.setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt().toInstant(UTC) : null);
    pref.setUpdatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().toInstant(UTC) : null);
    return pref;
  }
}
