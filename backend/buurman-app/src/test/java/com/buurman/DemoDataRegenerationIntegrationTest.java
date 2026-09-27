package com.buurman;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.EXPENSE_ALLOCATIONS;
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
