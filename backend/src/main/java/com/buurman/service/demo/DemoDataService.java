package com.buurman.service.demo;

import com.buurman.config.models.DemoDataProperties;
import com.buurman.dto.response.DemoDataResponse;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.AUDIT_LOG;
import static com.buurman.jooq.generated.Tables.CALENDAR_FEEDS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.CONTRACT_PAYMENT_INSTRUCTIONS;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.EMAIL_VERIFICATION_CODES;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.GENERATED_REPORTS;
import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;
import static com.buurman.jooq.generated.Tables.NOTIFICATION_OUTBOX;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PAYMENT_INSTRUCTIONS;
import static com.buurman.jooq.generated.Tables.PAYMENT_RECEIVALS;
import static com.buurman.jooq.generated.Tables.PHOTOS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_AMENITIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_OUTDOOR_AREAS;
import static com.buurman.jooq.generated.Tables.PROPERTY_TENANT_HISTORY;
import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.jooq.generated.Tables.TEAM_INVITATIONS;
import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;
import static com.buurman.jooq.generated.Tables.TENANTS;
import static com.buurman.jooq.generated.Tables.TENANT_ADDRESSES;
import static com.buurman.jooq.generated.Tables.USERS;
import static com.buurman.jooq.generated.Tables.USER_PREFERENCES;
import static com.buurman.jooq.generated.Tables.USER_TEAM_NOTIFICATION_PREFERENCES;

@Service
public class DemoDataService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private final DSLContext dsl;
    private final DemoDataProperties properties;
    private final DemoKeycloakSetup keycloakSetup;
    private final DemoTeamGenerator teamGenerator;
    private final DemoUserGenerator userGenerator;
    private final DemoTeamMemberGenerator teamMemberGenerator;
    private final DemoPropertyGenerator propertyGenerator;
    private final DemoTenantGenerator tenantGenerator;
    private final DemoContractGenerator contractGenerator;
    private final DemoPaymentGenerator paymentGenerator;
    private final DemoExpenseGenerator expenseGenerator;
    private final DemoPaymentInstructionGenerator paymentInstructionGenerator;
    private final DemoPhotoGenerator photoGenerator;
    private final DemoNotificationGenerator notificationGenerator;
    private final DemoDocumentGenerator documentGenerator;
    private final DemoAuditLogGenerator auditLogGenerator;
    private final Clock clock;

    private volatile Instant lastGeneratedAt;

    public DemoDataService(DSLContext dsl, DemoDataProperties properties,
                          DemoKeycloakSetup keycloakSetup, DemoTeamGenerator teamGenerator,
                          DemoUserGenerator userGenerator, DemoTeamMemberGenerator teamMemberGenerator,
                          DemoPropertyGenerator propertyGenerator, DemoTenantGenerator tenantGenerator,
                          DemoContractGenerator contractGenerator, DemoPaymentGenerator paymentGenerator,
                          DemoExpenseGenerator expenseGenerator, DemoPaymentInstructionGenerator paymentInstructionGenerator,
                          DemoPhotoGenerator photoGenerator, DemoNotificationGenerator notificationGenerator,
                          DemoDocumentGenerator documentGenerator, DemoAuditLogGenerator auditLogGenerator,
                          Clock clock) {
        this.dsl = dsl;
        this.properties = properties;
        this.keycloakSetup = keycloakSetup;
        this.teamGenerator = teamGenerator;
        this.userGenerator = userGenerator;
        this.teamMemberGenerator = teamMemberGenerator;
        this.propertyGenerator = propertyGenerator;
        this.tenantGenerator = tenantGenerator;
        this.contractGenerator = contractGenerator;
        this.paymentGenerator = paymentGenerator;
        this.expenseGenerator = expenseGenerator;
        this.paymentInstructionGenerator = paymentInstructionGenerator;
        this.photoGenerator = photoGenerator;
        this.notificationGenerator = notificationGenerator;
        this.documentGenerator = documentGenerator;
        this.auditLogGenerator = auditLogGenerator;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("Demo data loading is disabled");
            return;
        }

        int userCount = dsl.fetchCount(USERS);
        if (userCount == 0) {
            log.info("Empty database detected, auto-loading demo data...");
            generate();
        } else {
            log.info("Database already has {} users, skipping auto-load", userCount);
        }
    }

    public DemoDataResponse generate() {
        long startTime = clock.millis();
        log.info("Starting demo data generation...");

        DemoDataContext ctx = new DemoDataContext();

        // 1. Cleanup existing demo data
        cleanup();

        // 2. Create Keycloak users (external, non-transactional)
        keycloakSetup.createUsers(ctx);

        // 3. Generate database records (transactional)
        generateDatabaseRecords(ctx);

        // 4. Upload files to S3 (non-transactional, S3 + DB)
        try {
            photoGenerator.generate(ctx);
        } catch (Exception e) {
            log.warn("Photo generation failed (non-fatal): {}", e.getMessage());
        }
        try {
            documentGenerator.generate(ctx);
        } catch (Exception e) {
            log.warn("Document generation failed (non-fatal): {}", e.getMessage());
        }

        // 5. Force logout demo user so they get fresh session with reset data
        keycloakSetup.logoutDemoUser();

        long durationMs = clock.millis() - startTime;
        lastGeneratedAt = clock.instant();

        log.info("Demo data generation completed in {}ms: {} teams, {} users, {} properties, {} tenants, {} contracts, {} payments, {} expenses, {} notifications, {} documents",
                durationMs, ctx.getTeamsCreated(), ctx.getUsersCreated(), ctx.getPropertiesCreated(),
                ctx.getTenantsCreated(), ctx.getContractsCreated(), ctx.getPaymentsCreated(), ctx.getExpensesCreated(),
                ctx.getNotificationsCreated(), ctx.getDocumentsCreated());

        return new DemoDataResponse(
                ctx.getTeamsCreated(),
                ctx.getUsersCreated(),
                ctx.getPropertiesCreated(),
                ctx.getTenantsCreated(),
                ctx.getContractsCreated(),
                ctx.getPaymentsCreated(),
                ctx.getExpensesCreated(),
                ctx.getNotificationsCreated(),
                ctx.getDocumentsCreated(),
                durationMs
        );
    }

    @Transactional
    public void generateDatabaseRecords(DemoDataContext ctx) {
        teamGenerator.generate(ctx);
        userGenerator.generate(ctx);
        teamMemberGenerator.generate(ctx);
        propertyGenerator.generate(ctx);
        tenantGenerator.generate(ctx);
        contractGenerator.generate(ctx);
        paymentInstructionGenerator.generate(ctx);
        paymentGenerator.generate(ctx);
        expenseGenerator.generate(ctx);
        notificationGenerator.generate(ctx);
        auditLogGenerator.generate(ctx);
    }

    public void cleanup() {
        log.info("Cleaning up existing demo data...");

        // Find demo team IDs
        List<UUID> demoTeamIds = dsl.select(TEAMS.ID)
                .from(TEAMS)
                .where(TEAMS.SETTINGS.cast(String.class).contains("\"demoData\""))
                .fetch(TEAMS.ID);

        if (demoTeamIds.isEmpty()) {
            log.info("No existing demo data found");
            // Still try to clean up Keycloak users
            DemoDataContext cleanupCtx = new DemoDataContext();
            keycloakSetup.deleteUsers(cleanupCtx);
            return;
        }

        log.info("Found {} demo teams to clean up", demoTeamIds.size());

        cleanupDatabaseRecords(demoTeamIds);

        // Clean up Keycloak users
        DemoDataContext cleanupCtx = new DemoDataContext();
        keycloakSetup.deleteUsers(cleanupCtx);

        log.info("Demo data cleanup completed");
    }

    @Transactional
    public void cleanupDatabaseRecords(List<UUID> demoTeamIds) {
        // Delete in reverse FK dependency order

        // 1. Payment receivals (FK -> payments)
        int deleted = dsl.deleteFrom(PAYMENT_RECEIVALS)
                .where(PAYMENT_RECEIVALS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} payment receivals", deleted);

        // 2. Calendar feeds (FK -> various)
        deleted = dsl.deleteFrom(CALENDAR_FEEDS)
                .where(CALENDAR_FEEDS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} calendar feeds", deleted);

        // 3. Notification outbox (FK -> notifications)
        List<UUID> notificationIds = dsl.select(NOTIFICATIONS.ID)
                .from(NOTIFICATIONS)
                .where(NOTIFICATIONS.TEAM_ID.in(demoTeamIds))
                .fetch(NOTIFICATIONS.ID);
        if (!notificationIds.isEmpty()) {
            deleted = dsl.deleteFrom(NOTIFICATION_OUTBOX)
                    .where(NOTIFICATION_OUTBOX.NOTIFICATION_ID.in(notificationIds))
                    .execute();
            log.debug("Deleted {} notification outbox entries", deleted);
        }

        // 3b. Notifications
        deleted = dsl.deleteFrom(NOTIFICATIONS)
                .where(NOTIFICATIONS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} notifications", deleted);

        // 4. Audit log
        deleted = dsl.deleteFrom(AUDIT_LOG)
                .where(AUDIT_LOG.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} audit log entries", deleted);

        // 4. Generated reports
        deleted = dsl.deleteFrom(GENERATED_REPORTS)
                .where(GENERATED_REPORTS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} generated reports", deleted);

        // 5. Documents
        deleted = dsl.deleteFrom(DOCUMENTS)
                .where(DOCUMENTS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} documents", deleted);

        // 5b. Photos
        deleted = dsl.deleteFrom(PHOTOS)
                .where(PHOTOS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} photos", deleted);

        // 6. Contract payment instructions (FK -> contracts, payment_instructions)
        deleted = dsl.deleteFrom(CONTRACT_PAYMENT_INSTRUCTIONS)
                .where(CONTRACT_PAYMENT_INSTRUCTIONS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} contract payment instructions", deleted);

        // 7. Payments (FK -> contracts)
        deleted = dsl.deleteFrom(PAYMENTS)
                .where(PAYMENTS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} payments", deleted);

        // 8. Expenses (FK -> properties)
        deleted = dsl.deleteFrom(EXPENSES)
                .where(EXPENSES.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} expenses", deleted);

        // 9. Contracts (FK -> properties, tenants)
        deleted = dsl.deleteFrom(CONTRACTS)
                .where(CONTRACTS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} contracts", deleted);

        // 10. Payment instructions (FK -> users)
        deleted = dsl.deleteFrom(PAYMENT_INSTRUCTIONS)
                .where(PAYMENT_INSTRUCTIONS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} payment instructions", deleted);

        // 9. Property tenant history
        deleted = dsl.deleteFrom(PROPERTY_TENANT_HISTORY)
                .where(PROPERTY_TENANT_HISTORY.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} property tenant history entries", deleted);

        // 10. Tenant addresses (FK -> tenants)
        deleted = dsl.deleteFrom(TENANT_ADDRESSES)
                .where(TENANT_ADDRESSES.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} tenant addresses", deleted);

        // 11. Tenants
        deleted = dsl.deleteFrom(TENANTS)
                .where(TENANTS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} tenants", deleted);

        // 12. Property amenities
        deleted = dsl.deleteFrom(PROPERTY_AMENITIES)
                .where(PROPERTY_AMENITIES.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} property amenities", deleted);

        // 13. Property outdoor areas
        deleted = dsl.deleteFrom(PROPERTY_OUTDOOR_AREAS)
                .where(PROPERTY_OUTDOOR_AREAS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} property outdoor areas", deleted);

        // 14. Properties
        deleted = dsl.deleteFrom(PROPERTIES)
                .where(PROPERTIES.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} properties", deleted);

        // 15. User notification preferences
        deleted = dsl.deleteFrom(USER_TEAM_NOTIFICATION_PREFERENCES)
                .where(USER_TEAM_NOTIFICATION_PREFERENCES.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} user notification preferences", deleted);

        // 16. Team invitations
        deleted = dsl.deleteFrom(TEAM_INVITATIONS)
                .where(TEAM_INVITATIONS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} team invitations", deleted);

        // 17. Team members
        deleted = dsl.deleteFrom(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.TEAM_ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} team members", deleted);

        // 18. Nullify user references to demo teams, then delete demo users
        List<String> demoEmails = DemoUsers.ALL_EMAILS;
        dsl.update(USERS)
                .set(USERS.DEFAULT_TEAM_ID, (UUID) null)
                .set(USERS.ACTIVE_TEAM_ID, (UUID) null)
                .where(USERS.EMAIL.in(demoEmails))
                .execute();

        // Delete user preferences for demo users
        List<UUID> demoUserIds = dsl.select(USERS.ID)
                .from(USERS)
                .where(USERS.EMAIL.in(demoEmails))
                .fetch(USERS.ID);

        if (!demoUserIds.isEmpty()) {
            deleted = dsl.deleteFrom(EMAIL_VERIFICATION_CODES)
                    .where(EMAIL_VERIFICATION_CODES.USER_ID.in(demoUserIds))
                    .execute();
            log.debug("Deleted {} email verification codes", deleted);

            dsl.deleteFrom(USER_PREFERENCES)
                    .where(USER_PREFERENCES.USER_ID.in(demoUserIds))
                    .execute();
        }

        deleted = dsl.deleteFrom(USERS)
                .where(USERS.EMAIL.in(demoEmails))
                .execute();
        log.debug("Deleted {} users", deleted);

        // 19. Teams
        deleted = dsl.deleteFrom(TEAMS)
                .where(TEAMS.ID.in(demoTeamIds))
                .execute();
        log.debug("Deleted {} teams", deleted);
    }

    @Scheduled(cron = "${buurman.demo.cron:0 0 */12 * * *}")
    public void scheduledRegenerate() {
        if (!properties.enabled() || !properties.autoRegenerate()) {
            return;
        }
        log.info("Starting scheduled demo data regeneration");
        generate();
    }

    public Instant getLastGeneratedAt() {
        return lastGeneratedAt;
    }

    public boolean isEnabled() {
        return properties.enabled();
    }
}
