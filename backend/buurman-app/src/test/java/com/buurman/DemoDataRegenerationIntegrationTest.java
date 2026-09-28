package com.buurman;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.EXPENSE_ALLOCATIONS;
import static com.buurman.jooq.generated.Tables.NOTIFICATIONS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.TEAMS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.buurman.repository.UnitRepository;
import com.buurman.service.demo.DemoDataContext;
import com.buurman.service.demo.DemoDataService;
import com.buurman.service.demo.DemoUsers;

/**
 * QA register section F item 10: regenerating demo data twice must not accumulate duplicate units,
 * properties or contracts. This is the test that would have caught both BUUR-106 regressions found
 * on this branch — {@code ContractRepository.save()} and {@code DemoContractGenerator} each once
 * forgot to set {@code contracts.unit_id} (NOT NULL since V070) — and it covers the 20-step
 * FK-ordered cleanup {@link DemoDataService#cleanupDatabaseRecords} performs before every
 * regeneration.
 *
 * <p>Exercises only the database-writing half of demo data generation ({@link
 * DemoDataService#generateDatabaseRecords} / {@link DemoDataService#cleanupDatabaseRecords}), not
 * the full {@link DemoDataService#generate()} orchestration — that also calls out to Keycloak and
 * S3, neither of which is available against this module's Testcontainers-only test profile. Demo
 * users normally get their {@code keycloak_id} from that Keycloak call; this test fakes one per
 * user instead, since {@code users.keycloak_id} is {@code NOT NULL}.
 */
@SpringBootTest
@ActiveProfiles("test")
class DemoDataRegenerationIntegrationTest {

  @Autowired private DemoDataService demoDataService;
  @Autowired private UnitRepository unitRepository;
  @Autowired private DSLContext dsl;

  @Test
  void regeneratingDemoDataTwiceLeavesNoDuplicates() {
    UUID firstDemoTeamId = resetAndGenerate();
    int unitsAfterFirst = unitRepository.countActiveByTeamId(firstDemoTeamId);
    int propertiesAfterFirst = countByTeam(PROPERTIES.TEAM_ID, firstDemoTeamId);
    int contractsAfterFirst = countByTeam(CONTRACTS.TEAM_ID, firstDemoTeamId);
    int allocationsAfterFirst = countByTeam(EXPENSE_ALLOCATIONS.TEAM_ID, firstDemoTeamId);

    // A curated multi-unit building must actually have been created, or this test would pass
    // vacuously even if DemoUnitGenerator regressed back to one implicit unit per property.
    assertThat(unitsAfterFirst).isGreaterThan(propertiesAfterFirst);
    assertThat(allocationsAfterFirst).isGreaterThan(0);
    assertThat(contractsAfterFirst).isGreaterThan(0);

    UUID secondDemoTeamId = resetAndGenerate();
    int unitsAfterSecond = unitRepository.countActiveByTeamId(secondDemoTeamId);
    int propertiesAfterSecond = countByTeam(PROPERTIES.TEAM_ID, secondDemoTeamId);
    int contractsAfterSecond = countByTeam(CONTRACTS.TEAM_ID, secondDemoTeamId);
    int allocationsAfterSecond = countByTeam(EXPENSE_ALLOCATIONS.TEAM_ID, secondDemoTeamId);

    // Units, properties and allocated expenses are a fixed, curated shape per regeneration, so
    // these must match exactly. Contract *counts* are not asserted for equality: chain length is
    // itself randomized (com.buurman.service.demo.DemoContractGenerator#computeChainLength) off a
    // Random field that — like every demo generator's — is a Spring-singleton instance whose state
    // carries over between successive regenerations, so the exact count legitimately varies run to
    // run. What must not vary is the FK-safety this whole test is really for: a second regeneration
    // must complete without a NOT NULL/unique violation, which a failed test run would have caught.
    assertThat(unitsAfterSecond).isEqualTo(unitsAfterFirst);
    assertThat(propertiesAfterSecond).isEqualTo(propertiesAfterFirst);
    assertThat(contractsAfterSecond).isGreaterThan(0);
    assertThat(allocationsAfterSecond).isEqualTo(allocationsAfterFirst);

    // The old team's rows must be fully gone, not just superseded — proves the 20-step cleanup
    // actually ran rather than the counts merely happening to match by coincidence.
    assertThat(unitRepository.countActiveByTeamId(firstDemoTeamId)).isZero();
    assertThat(countByTeam(PROPERTIES.TEAM_ID, firstDemoTeamId)).isZero();
  }

  /**
   * The communications timeline on a payment or contract reads {@code notifications.payment_id} and
   * {@code notifications.contract_id}. The demo generator writes notification rows straight through
   * JOOQ rather than through {@code NotificationService}, so it does not pick those columns up for
   * free the way the nine production call sites do — it left both NULL and every demo timeline was
   * empty, which reads as a broken feature rather than as missing data.
   *
   * <p>Asserts per notification type rather than in aggregate: a single linked row would satisfy
   * "some are linked" while the other kinds silently regressed.
   */
  @Test
  void demoNotificationsLinkToThePaymentOrContractTheyAreAbout() {
    UUID teamId = resetAndGenerate();

    assertThat(typesInTeam(teamId))
        .as("demo data must cover both the payment-linked and contract-linked kinds")
        .contains(
            "PAYMENT_REMINDER", "PAYMENT_PAID", "CONTRACT_CREATED", "CONTRACT_EXPIRY", "WELCOME");

    // A payment notification is about the payment AND its contract, mirroring what
    // PaymentService and PaymentReminderService record, so the contract timeline shows it
    // without having to join through payments.
    for (String type : List.of("PAYMENT_REMINDER", "PAYMENT_PAID")) {
      assertThat(countWhere(teamId, type, NOTIFICATIONS.PAYMENT_ID.isNull()))
          .as("%s notifications with no payment link", type)
          .isZero();
      assertThat(countWhere(teamId, type, NOTIFICATIONS.CONTRACT_ID.isNull()))
          .as("%s notifications with no contract link", type)
          .isZero();
    }

    for (String type : List.of("CONTRACT_CREATED", "CONTRACT_STATUS_CHANGED", "CONTRACT_EXPIRY")) {
      assertThat(countWhere(teamId, type, NOTIFICATIONS.CONTRACT_ID.isNull()))
          .as("%s notifications with no contract link", type)
          .isZero();
      assertThat(countWhere(teamId, type, NOTIFICATIONS.PAYMENT_ID.isNotNull()))
          .as("%s is not about a payment", type)
          .isZero();
    }

    // Neither a welcome email nor a property-created email is about a payment or a contract.
    for (String type : List.of("WELCOME", "PROPERTY_CREATED")) {
      assertThat(
              countWhere(
                  teamId,
                  type,
                  NOTIFICATIONS.PAYMENT_ID.isNotNull().or(NOTIFICATIONS.CONTRACT_ID.isNotNull())))
          .as("%s must not claim to be about a payment or contract", type)
          .isZero();
    }

    // The pair must agree: a notification linked to a payment must name that payment's own
    // contract, or the same row would appear on two unrelated timelines.
    Integer mismatched =
        dsl.selectCount()
            .from(NOTIFICATIONS)
            .join(PAYMENTS)
            .on(PAYMENTS.ID.eq(NOTIFICATIONS.PAYMENT_ID))
            .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
            .and(NOTIFICATIONS.CONTRACT_ID.ne(PAYMENTS.CONTRACT_ID))
            .fetchOne(0, Integer.class);
    assertThat(mismatched).as("payment link and contract link disagree").isZero();

    // Multi-tenancy: a link must never reach out of the team that owns the notification.
    assertThat(
            dsl.selectCount()
                .from(NOTIFICATIONS)
                .join(PAYMENTS)
                .on(PAYMENTS.ID.eq(NOTIFICATIONS.PAYMENT_ID))
                .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.TEAM_ID.ne(NOTIFICATIONS.TEAM_ID))
                .fetchOne(0, Integer.class))
        .as("notification linked to another team's payment")
        .isZero();
    assertThat(
            dsl.selectCount()
                .from(NOTIFICATIONS)
                .join(CONTRACTS)
                .on(CONTRACTS.ID.eq(NOTIFICATIONS.CONTRACT_ID))
                .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.TEAM_ID.ne(NOTIFICATIONS.TEAM_ID))
                .fetchOne(0, Integer.class))
        .as("notification linked to another team's contract")
        .isZero();
  }

  private List<String> typesInTeam(UUID teamId) {
    return dsl.selectDistinct(NOTIFICATIONS.NOTIFICATION_TYPE)
        .from(NOTIFICATIONS)
        .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
        .fetch(NOTIFICATIONS.NOTIFICATION_TYPE);
  }

  private int countWhere(UUID teamId, String type, org.jooq.Condition condition) {
    Integer count =
        dsl.selectCount()
            .from(NOTIFICATIONS)
            .where(NOTIFICATIONS.TEAM_ID.eq(teamId))
            .and(NOTIFICATIONS.NOTIFICATION_TYPE.eq(type))
            .and(condition)
            .fetchOne(0, Integer.class);
    return count == null ? 0 : count;
  }

  /** Cleans up any existing demo data, then regenerates it, returning the new "demo-team" id. */
  private UUID resetAndGenerate() {
    List<UUID> existingDemoTeamIds =
        dsl.select(TEAMS.ID).from(TEAMS).where(TEAMS.DEMO.isTrue()).fetch(TEAMS.ID);
    if (!existingDemoTeamIds.isEmpty()) {
      demoDataService.cleanupDatabaseRecords(existingDemoTeamIds);
    }

    DemoDataContext ctx = new DemoDataContext();
    for (DemoUsers.DemoUser user : DemoUsers.ALL_USERS) {
      ctx.getKeycloakIds().put(user.email(), "test-keycloak-" + UUID.randomUUID());
    }

    demoDataService.generateDatabaseRecords(ctx);

    return java.util.Objects.requireNonNull(
        ctx.getTeamIds().get("demo-team"), "demo-team was not created by generateDatabaseRecords");
  }

  private int countByTeam(org.jooq.TableField<?, UUID> teamIdField, UUID teamId) {
    Integer count =
        dsl.selectCount()
            .from(teamIdField.getTable())
            .where(teamIdField.eq(teamId))
            .fetchOne(0, Integer.class);
    return count == null ? 0 : count;
  }
}
