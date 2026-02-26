package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.AUDIT_LOG;
import static com.buurman.jooq.generated.Tables.CALENDAR_FEEDS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.CONTRACT_PAYMENT_INSTRUCTIONS;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.EMAIL_VERIFICATION_CODES;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.FINANCING_PAYMENTS;
import static com.buurman.jooq.generated.Tables.GENERATED_REPORTS;
import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;
import static com.buurman.jooq.generated.Tables.NOTIFICATION_OUTBOX;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PAYMENT_INSTRUCTIONS;
import static com.buurman.jooq.generated.Tables.PAYMENT_RECEIVALS;
import static com.buurman.jooq.generated.Tables.PHONE_VERIFICATION_CODES;
import static com.buurman.jooq.generated.Tables.PHOTOS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_ACQUISITIONS;
import static com.buurman.jooq.generated.Tables.PROPERTY_AGRICULTURAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_AMENITIES;
import static com.buurman.jooq.generated.Tables.PROPERTY_COMMERCIAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_FEES;
import static com.buurman.jooq.generated.Tables.PROPERTY_FINANCINGS;
import static com.buurman.jooq.generated.Tables.PROPERTY_INDUSTRIAL_DETAILS;
import static com.buurman.jooq.generated.Tables.PROPERTY_INSURANCES;
import static com.buurman.jooq.generated.Tables.PROPERTY_OUTDOOR_AREAS;
import static com.buurman.jooq.generated.Tables.PROPERTY_TAXES;
import static com.buurman.jooq.generated.Tables.PROPERTY_TENANT_HISTORY;
import static com.buurman.jooq.generated.Tables.PROPERTY_VALUATIONS;
import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.jooq.generated.Tables.TEAM_INVITATIONS;
import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;
import static com.buurman.jooq.generated.Tables.TENANTS;
import static com.buurman.jooq.generated.Tables.TENANT_ADDRESSES;
import static com.buurman.jooq.generated.Tables.USERS;
import static com.buurman.jooq.generated.Tables.USER_PREFERENCES;
import static com.buurman.jooq.generated.Tables.USER_TEAM_NOTIFICATION_PREFERENCES;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.config.models.DemoDataProperties;
import com.buurman.dto.response.DemoDataResponse;
import com.buurman.service.S3StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DemoDataService {

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
  private final S3StorageService s3StorageService;
  private final Clock clock;

  @Transactional
  public void generate() {
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

    log.info(
        "Demo data generation completed in {}ms: {} teams, {} users, {} properties, {} tenants, {}"
            + " contracts, {} payments, {} expenses, {} notifications, {} documents",
        durationMs,
        ctx.getTeamsCreated(),
        ctx.getUsersCreated(),
        ctx.getPropertiesCreated(),
        ctx.getTenantsCreated(),
        ctx.getContractsCreated(),
        ctx.getPaymentsCreated(),
        ctx.getExpensesCreated(),
        ctx.getNotificationsCreated(),
        ctx.getDocumentsCreated());

    new DemoDataResponse(
        ctx.getTeamsCreated(),
        ctx.getUsersCreated(),
        ctx.getPropertiesCreated(),
        ctx.getTenantsCreated(),
        ctx.getContractsCreated(),
        ctx.getPaymentsCreated(),
        ctx.getExpensesCreated(),
        ctx.getNotificationsCreated(),
        ctx.getDocumentsCreated(),
        durationMs);
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

  @Transactional
  public void cleanup() {
    log.info("Cleaning up existing demo data...");

    // Find demo team IDs
    List<UUID> demoTeamIds =
        dsl.select(TEAMS.ID).from(TEAMS).where(TEAMS.DEMO.isTrue()).fetch(TEAMS.ID);

    if (demoTeamIds.isEmpty()) {
      log.info("No existing demo data found");
      // Still try to clean up Keycloak users
      DemoDataContext cleanupCtx = new DemoDataContext();
      keycloakSetup.deleteUsers(cleanupCtx);
      return;
    }

    log.info("Found {} demo teams to clean up", demoTeamIds.size());

    // Collect S3 file keys before deleting DB records
    List<String> s3Keys = collectS3FileKeys(demoTeamIds);

    cleanupDatabaseRecords(demoTeamIds);

    // Delete S3 files (non-transactional, after DB cleanup)
    deleteS3Files(s3Keys);

    // Clean up Keycloak users
    DemoDataContext cleanupCtx = new DemoDataContext();
    keycloakSetup.deleteUsers(cleanupCtx);

    log.info("Demo data cleanup completed");
  }

  @Transactional
  public void cleanupDatabaseRecords(List<UUID> demoTeamIds) {
    // Delete in reverse FK dependency order

    // 1. Payment receivals (FK -> payments)
    int deleted =
        dsl.deleteFrom(PAYMENT_RECEIVALS)
            .where(PAYMENT_RECEIVALS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} payment receivals", deleted);

    // 2. Calendar feeds (FK -> various)
    deleted =
        dsl.deleteFrom(CALENDAR_FEEDS).where(CALENDAR_FEEDS.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} calendar feeds", deleted);

    // 3. Notification outbox (FK -> notifications)
    List<UUID> notificationIds =
        dsl.select(NOTIFICATIONS.ID)
            .from(NOTIFICATIONS)
            .where(NOTIFICATIONS.TEAM_ID.in(demoTeamIds))
            .fetch(NOTIFICATIONS.ID);
    if (!notificationIds.isEmpty()) {
      deleted =
          dsl.deleteFrom(NOTIFICATION_OUTBOX)
              .where(NOTIFICATION_OUTBOX.NOTIFICATION_ID.in(notificationIds))
              .execute();
      log.debug("Deleted {} notification outbox entries", deleted);
    }

    // 3b. Notifications
    deleted = dsl.deleteFrom(NOTIFICATIONS).where(NOTIFICATIONS.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} notifications", deleted);

    // 4. Audit log
    deleted = dsl.deleteFrom(AUDIT_LOG).where(AUDIT_LOG.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} audit log entries", deleted);

    // 4. Generated reports
    deleted =
        dsl.deleteFrom(GENERATED_REPORTS)
            .where(GENERATED_REPORTS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} generated reports", deleted);

    // 5. Documents
    deleted = dsl.deleteFrom(DOCUMENTS).where(DOCUMENTS.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} documents", deleted);

    // 5b. Photos
    deleted = dsl.deleteFrom(PHOTOS).where(PHOTOS.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} photos", deleted);

    // 6. Contract payment instructions (FK -> contracts, payment_instructions)
    deleted =
        dsl.deleteFrom(CONTRACT_PAYMENT_INSTRUCTIONS)
            .where(CONTRACT_PAYMENT_INSTRUCTIONS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} contract payment instructions", deleted);

    // 7. Payments (FK -> contracts)
    deleted = dsl.deleteFrom(PAYMENTS).where(PAYMENTS.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} payments", deleted);

    // 8. Expenses (FK -> properties)
    deleted = dsl.deleteFrom(EXPENSES).where(EXPENSES.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} expenses", deleted);

    // 9a. Contract rent periods (FK -> contracts)
    deleted =
        dsl.deleteFrom(DSL.table("contract_rent_periods"))
            .where(DSL.field("team_id", java.util.UUID.class).in(demoTeamIds))
            .execute();
    log.debug("Deleted {} contract rent periods", deleted);

    // 9b. Contract parties (FK -> contracts, tenants)
    deleted =
        dsl.deleteFrom(DSL.table("contract_parties"))
            .where(DSL.field("team_id", java.util.UUID.class).in(demoTeamIds))
            .execute();
    log.debug("Deleted {} contract parties", deleted);

    // 9c. Contracts (FK -> properties, tenants)
    deleted = dsl.deleteFrom(CONTRACTS).where(CONTRACTS.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} contracts", deleted);

    // 10. Payment instructions (FK -> users)
    deleted =
        dsl.deleteFrom(PAYMENT_INSTRUCTIONS)
            .where(PAYMENT_INSTRUCTIONS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} payment instructions", deleted);

    // 9. Property tenant history
    deleted =
        dsl.deleteFrom(PROPERTY_TENANT_HISTORY)
            .where(PROPERTY_TENANT_HISTORY.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property tenant history entries", deleted);

    // 10. Tenant addresses (FK -> tenants)
    deleted =
        dsl.deleteFrom(TENANT_ADDRESSES).where(TENANT_ADDRESSES.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} tenant addresses", deleted);

    // 11. Tenants
    deleted = dsl.deleteFrom(TENANTS).where(TENANTS.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} tenants", deleted);

    // 12. Property amenities
    deleted =
        dsl.deleteFrom(PROPERTY_AMENITIES)
            .where(PROPERTY_AMENITIES.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property amenities", deleted);

    // 13. Property outdoor areas
    deleted =
        dsl.deleteFrom(PROPERTY_OUTDOOR_AREAS)
            .where(PROPERTY_OUTDOOR_AREAS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property outdoor areas", deleted);

    // 13b. Property category-specific details
    deleted =
        dsl.deleteFrom(PROPERTY_COMMERCIAL_DETAILS)
            .where(PROPERTY_COMMERCIAL_DETAILS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property commercial details", deleted);

    deleted =
        dsl.deleteFrom(PROPERTY_INDUSTRIAL_DETAILS)
            .where(PROPERTY_INDUSTRIAL_DETAILS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property industrial details", deleted);

    deleted =
        dsl.deleteFrom(PROPERTY_AGRICULTURAL_DETAILS)
            .where(PROPERTY_AGRICULTURAL_DETAILS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property agricultural details", deleted);

    // 13c. Property financials (FK -> properties)
    deleted =
        dsl.deleteFrom(FINANCING_PAYMENTS)
            .where(FINANCING_PAYMENTS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} financing payments", deleted);

    deleted =
        dsl.deleteFrom(PROPERTY_FINANCINGS)
            .where(PROPERTY_FINANCINGS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property financings", deleted);

    deleted =
        dsl.deleteFrom(PROPERTY_ACQUISITIONS)
            .where(PROPERTY_ACQUISITIONS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property acquisitions", deleted);

    deleted =
        dsl.deleteFrom(PROPERTY_VALUATIONS)
            .where(PROPERTY_VALUATIONS.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property valuations", deleted);

    deleted =
        dsl.deleteFrom(PROPERTY_INSURANCES)
            .where(PROPERTY_INSURANCES.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} property insurances", deleted);

    deleted =
        dsl.deleteFrom(PROPERTY_TAXES).where(PROPERTY_TAXES.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} property taxes", deleted);

    deleted = dsl.deleteFrom(PROPERTY_FEES).where(PROPERTY_FEES.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} property fees", deleted);

    // 14. Properties
    deleted = dsl.deleteFrom(PROPERTIES).where(PROPERTIES.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} properties", deleted);

    // 15. User notification preferences
    deleted =
        dsl.deleteFrom(USER_TEAM_NOTIFICATION_PREFERENCES)
            .where(USER_TEAM_NOTIFICATION_PREFERENCES.TEAM_ID.in(demoTeamIds))
            .execute();
    log.debug("Deleted {} user notification preferences", deleted);

    // 16. Team invitations
    deleted =
        dsl.deleteFrom(TEAM_INVITATIONS).where(TEAM_INVITATIONS.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} team invitations", deleted);

    // 17. Team members
    deleted = dsl.deleteFrom(TEAM_MEMBERS).where(TEAM_MEMBERS.TEAM_ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} team members", deleted);

    // 18. Nullify user references to demo teams, then delete demo users
    List<String> demoEmails = DemoUsers.ALL_EMAILS;
    List<UUID> demoUserIds =
        dsl.select(USERS.ID).from(USERS).where(USERS.EMAIL.in(demoEmails)).fetch(USERS.ID);

    dsl.update(USERS)
        .set(USERS.DEFAULT_TEAM_ID, (UUID) null)
        .set(USERS.ACTIVE_TEAM_ID, (UUID) null)
        .where(USERS.EMAIL.in(demoEmails))
        .execute();

    // Nullify created_by/updated_by references to demo users in remaining rows
    if (!demoUserIds.isEmpty()) {
      dsl.update(PROPERTIES)
          .set(PROPERTIES.CREATED_BY, (UUID) null)
          .where(PROPERTIES.CREATED_BY.in(demoUserIds))
          .execute();
      dsl.update(PROPERTIES)
          .set(PROPERTIES.UPDATED_BY, (UUID) null)
          .where(PROPERTIES.UPDATED_BY.in(demoUserIds))
          .execute();
    }

    if (!demoUserIds.isEmpty()) {
      deleted =
          dsl.deleteFrom(PHONE_VERIFICATION_CODES)
              .where(PHONE_VERIFICATION_CODES.USER_ID.in(demoUserIds))
              .execute();
      log.debug("Deleted {} phone verification codes", deleted);

      deleted =
          dsl.deleteFrom(EMAIL_VERIFICATION_CODES)
              .where(EMAIL_VERIFICATION_CODES.USER_ID.in(demoUserIds))
              .execute();
      log.debug("Deleted {} email verification codes", deleted);

      dsl.deleteFrom(USER_PREFERENCES).where(USER_PREFERENCES.USER_ID.in(demoUserIds)).execute();
    }

    deleted = dsl.deleteFrom(USERS).where(USERS.EMAIL.in(demoEmails)).execute();
    log.debug("Deleted {} users", deleted);

    // 19. Teams
    deleted = dsl.deleteFrom(TEAMS).where(TEAMS.ID.in(demoTeamIds)).execute();
    log.debug("Deleted {} teams", deleted);
  }

  private List<String> collectS3FileKeys(List<UUID> demoTeamIds) {
    List<String> keys = new ArrayList<>();

    keys.addAll(
        dsl.select(DOCUMENTS.FILE_KEY)
            .from(DOCUMENTS)
            .where(DOCUMENTS.TEAM_ID.in(demoTeamIds))
            .and(DOCUMENTS.FILE_KEY.isNotNull())
            .fetch(DOCUMENTS.FILE_KEY));

    keys.addAll(
        dsl.select(PHOTOS.FILE_KEY)
            .from(PHOTOS)
            .where(PHOTOS.TEAM_ID.in(demoTeamIds))
            .and(PHOTOS.FILE_KEY.isNotNull())
            .fetch(PHOTOS.FILE_KEY));

    keys.addAll(
        dsl.select(PHOTOS.THUMBNAIL_FILE_KEY)
            .from(PHOTOS)
            .where(PHOTOS.TEAM_ID.in(demoTeamIds))
            .and(PHOTOS.THUMBNAIL_FILE_KEY.isNotNull())
            .fetch(PHOTOS.THUMBNAIL_FILE_KEY));

    keys.addAll(
        dsl.select(GENERATED_REPORTS.FILE_KEY)
            .from(GENERATED_REPORTS)
            .where(GENERATED_REPORTS.TEAM_ID.in(demoTeamIds))
            .and(GENERATED_REPORTS.FILE_KEY.isNotNull())
            .fetch(GENERATED_REPORTS.FILE_KEY));

    log.info("Collected {} S3 file keys to delete", keys.size());
    return keys;
  }

  private void deleteS3Files(List<String> keys) {
    int deleted = 0;
    for (String key : keys) {
      try {
        s3StorageService.deleteFile(key);
        deleted++;
      } catch (Exception e) {
        log.warn("Failed to delete S3 file {}: {}", key, e.getMessage());
      }
    }
    log.info("Deleted {}/{} S3 files", deleted, keys.size());
  }

  @Transactional
  public void scheduledRegenerate() {
    if (!properties.enabled()) {
      return;
    }
    log.info("Starting scheduled demo data regeneration");
    generate();
  }

  public boolean isEnabled() {
    return properties.enabled();
  }
}
