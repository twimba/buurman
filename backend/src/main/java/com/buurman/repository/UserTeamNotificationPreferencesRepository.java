package com.buurman.repository;

import com.buurman.domain.UserTeamNotificationPreferences;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.USER_TEAM_NOTIFICATION_PREFERENCES;

@Repository
public class UserTeamNotificationPreferencesRepository {

    private final DSLContext dsl;

    public UserTeamNotificationPreferencesRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<UserTeamNotificationPreferences> findByUserIdAndTeamId(UUID userId, UUID teamId) {
        return dsl.selectFrom(USER_TEAM_NOTIFICATION_PREFERENCES)
                .where(USER_TEAM_NOTIFICATION_PREFERENCES.USER_ID.eq(userId))
                .and(USER_TEAM_NOTIFICATION_PREFERENCES.TEAM_ID.eq(teamId))
                .fetchOptional()
                .map(this::toDomain);
    }

    public UserTeamNotificationPreferences save(UserTeamNotificationPreferences prefs) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (prefs.getId() == null) {
            UUID newId = UUID.randomUUID();
            dsl.insertInto(USER_TEAM_NOTIFICATION_PREFERENCES)
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.ID, newId)
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.USER_ID, prefs.getUserId())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.TEAM_ID, prefs.getTeamId())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.PAYMENT_REMINDERS, prefs.isPaymentReminders())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.CONTRACT_EXPIRY_ALERTS, prefs.isContractExpiryAlerts())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.NEW_MEMBER_NOTIFICATIONS, prefs.isNewMemberNotifications())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.WEEKLY_SUMMARY, prefs.isWeeklySummary())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.CREATED_AT, now)
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.UPDATED_AT, now)
                    .execute();
            prefs.setId(newId);
            prefs.setCreatedAt(now.toInstant(ZoneOffset.UTC));
            prefs.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            dsl.update(USER_TEAM_NOTIFICATION_PREFERENCES)
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.PAYMENT_REMINDERS, prefs.isPaymentReminders())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.CONTRACT_EXPIRY_ALERTS, prefs.isContractExpiryAlerts())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.NEW_MEMBER_NOTIFICATIONS, prefs.isNewMemberNotifications())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.WEEKLY_SUMMARY, prefs.isWeeklySummary())
                    .set(USER_TEAM_NOTIFICATION_PREFERENCES.UPDATED_AT, now)
                    .where(USER_TEAM_NOTIFICATION_PREFERENCES.ID.eq(prefs.getId()))
                    .execute();
            prefs.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }
        return prefs;
    }

    private UserTeamNotificationPreferences toDomain(
            com.buurman.jooq.generated.tables.records.UserTeamNotificationPreferencesRecord record) {
        UserTeamNotificationPreferences prefs = new UserTeamNotificationPreferences();
        prefs.setId(record.getId());
        prefs.setUserId(record.getUserId());
        prefs.setTeamId(record.getTeamId());
        prefs.setPaymentReminders(record.getPaymentReminders());
        prefs.setContractExpiryAlerts(record.getContractExpiryAlerts());
        prefs.setNewMemberNotifications(record.getNewMemberNotifications());
        prefs.setWeeklySummary(record.getWeeklySummary());
        prefs.setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt().toInstant(ZoneOffset.UTC) : null);
        prefs.setUpdatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().toInstant(ZoneOffset.UTC) : null);
        return prefs;
    }
}
