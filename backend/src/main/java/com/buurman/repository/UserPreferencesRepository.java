package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.USER_PREFERENCES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.UserPreferences;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserPreferencesRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public Optional<UserPreferences> findByUserId(UUID userId) {
    return dsl.selectFrom(USER_PREFERENCES)
        .where(USER_PREFERENCES.USER_ID.eq(userId))
        .fetchOptional()
        .map(this::toDomain);
  }

  public UserPreferences save(UserPreferences prefs) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (prefs.getId() == null) {
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt = LocalDateTime.ofInstant(prefs.getCreatedAt(), UTC);
      LocalDateTime updatedAt = LocalDateTime.ofInstant(prefs.getUpdatedAt(), UTC);

      dsl.insertInto(USER_PREFERENCES)
          .set(USER_PREFERENCES.ID, newId)
          .set(USER_PREFERENCES.USER_ID, prefs.getUserId())
          .set(USER_PREFERENCES.THEME, prefs.getTheme())
          .set(USER_PREFERENCES.LANGUAGE, prefs.getLanguage())
          .set(USER_PREFERENCES.TIMEZONE, prefs.getTimezone())
          .set(USER_PREFERENCES.DATE_FORMAT, prefs.getDateFormat())
          .set(USER_PREFERENCES.CURRENCY_FORMAT, prefs.getCurrencyFormat().orElse(null))
          .set(USER_PREFERENCES.EMAIL_NOTIFICATIONS, prefs.isEmailNotifications())
          .set(USER_PREFERENCES.IN_APP_NOTIFICATIONS, prefs.isInAppNotifications())
          .set(USER_PREFERENCES.SMS_NOTIFICATIONS, prefs.isSmsNotifications())
          .set(USER_PREFERENCES.CREATED_AT, createdAt)
          .set(USER_PREFERENCES.UPDATED_AT, updatedAt)
          .execute();
      prefs.setId(newId);
      prefs.setCreatedAt(createdAt.toInstant(UTC));
      prefs.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      LocalDateTime updatedAt = LocalDateTime.ofInstant(prefs.getUpdatedAt(), UTC);

      dsl.update(USER_PREFERENCES)
          .set(USER_PREFERENCES.THEME, prefs.getTheme())
          .set(USER_PREFERENCES.LANGUAGE, prefs.getLanguage())
          .set(USER_PREFERENCES.TIMEZONE, prefs.getTimezone())
          .set(USER_PREFERENCES.DATE_FORMAT, prefs.getDateFormat())
          .set(USER_PREFERENCES.CURRENCY_FORMAT, prefs.getCurrencyFormat().orElse(null))
          .set(USER_PREFERENCES.EMAIL_NOTIFICATIONS, prefs.isEmailNotifications())
          .set(USER_PREFERENCES.IN_APP_NOTIFICATIONS, prefs.isInAppNotifications())
          .set(USER_PREFERENCES.SMS_NOTIFICATIONS, prefs.isSmsNotifications())
          .set(USER_PREFERENCES.UPDATED_AT, updatedAt)
          .where(USER_PREFERENCES.ID.eq(prefs.getId()))
          .execute();
      prefs.setUpdatedAt(updatedAt.toInstant(UTC));
    }
    return prefs;
  }

  private UserPreferences toDomain(
      com.buurman.jooq.generated.tables.records.UserPreferencesRecord record) {
    UserPreferences prefs = new UserPreferences();
    prefs.setId(record.getId());
    prefs.setUserId(record.getUserId());
    prefs.setTheme(record.getTheme());
    prefs.setLanguage(record.getLanguage());
    prefs.setTimezone(record.getTimezone());
    prefs.setDateFormat(record.getDateFormat());
    prefs.setCurrencyFormat(Optional.ofNullable(record.getCurrencyFormat()));
    prefs.setEmailNotifications(record.getEmailNotifications());
    prefs.setInAppNotifications(record.getInAppNotifications());
    prefs.setSmsNotifications(record.getSmsNotifications());
    prefs.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    prefs.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    return prefs;
  }
}
