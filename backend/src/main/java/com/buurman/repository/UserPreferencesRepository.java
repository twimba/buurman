package com.buurman.repository;

import com.buurman.domain.UserPreferences;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.USER_PREFERENCES;

@Repository
public class UserPreferencesRepository {

    private final DSLContext dsl;

    public UserPreferencesRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<UserPreferences> findByUserId(UUID userId) {
        return dsl.selectFrom(USER_PREFERENCES)
                .where(USER_PREFERENCES.USER_ID.eq(userId))
                .fetchOptional()
                .map(this::toDomain);
    }

    public UserPreferences save(UserPreferences prefs) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (prefs.getId() == null) {
            UUID newId = UUID.randomUUID();
            dsl.insertInto(USER_PREFERENCES)
                    .set(USER_PREFERENCES.ID, newId)
                    .set(USER_PREFERENCES.USER_ID, prefs.getUserId())
                    .set(USER_PREFERENCES.THEME, prefs.getTheme())
                    .set(USER_PREFERENCES.LANGUAGE, prefs.getLanguage())
                    .set(USER_PREFERENCES.TIMEZONE, prefs.getTimezone())
                    .set(USER_PREFERENCES.DATE_FORMAT, prefs.getDateFormat())
                    .set(USER_PREFERENCES.CURRENCY_FORMAT, prefs.getCurrencyFormat())
                    .set(USER_PREFERENCES.EMAIL_NOTIFICATIONS, prefs.isEmailNotifications())
                    .set(USER_PREFERENCES.IN_APP_NOTIFICATIONS, prefs.isInAppNotifications())
                    .set(USER_PREFERENCES.CREATED_AT, now)
                    .set(USER_PREFERENCES.UPDATED_AT, now)
                    .execute();
            prefs.setId(newId);
            prefs.setCreatedAt(now.toInstant(ZoneOffset.UTC));
            prefs.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            dsl.update(USER_PREFERENCES)
                    .set(USER_PREFERENCES.THEME, prefs.getTheme())
                    .set(USER_PREFERENCES.LANGUAGE, prefs.getLanguage())
                    .set(USER_PREFERENCES.TIMEZONE, prefs.getTimezone())
                    .set(USER_PREFERENCES.DATE_FORMAT, prefs.getDateFormat())
                    .set(USER_PREFERENCES.CURRENCY_FORMAT, prefs.getCurrencyFormat())
                    .set(USER_PREFERENCES.EMAIL_NOTIFICATIONS, prefs.isEmailNotifications())
                    .set(USER_PREFERENCES.IN_APP_NOTIFICATIONS, prefs.isInAppNotifications())
                    .set(USER_PREFERENCES.UPDATED_AT, now)
                    .where(USER_PREFERENCES.ID.eq(prefs.getId()))
                    .execute();
            prefs.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }
        return prefs;
    }

    private UserPreferences toDomain(com.buurman.jooq.generated.tables.records.UserPreferencesRecord record) {
        UserPreferences prefs = new UserPreferences();
        prefs.setId(record.getId());
        prefs.setUserId(record.getUserId());
        prefs.setTheme(record.getTheme());
        prefs.setLanguage(record.getLanguage());
        prefs.setTimezone(record.getTimezone());
        prefs.setDateFormat(record.getDateFormat());
        prefs.setCurrencyFormat(record.getCurrencyFormat());
        prefs.setEmailNotifications(record.getEmailNotifications());
        prefs.setInAppNotifications(record.getInAppNotifications());
        prefs.setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt().toInstant(ZoneOffset.UTC) : null);
        prefs.setUpdatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().toInstant(ZoneOffset.UTC) : null);
        return prefs;
    }
}
