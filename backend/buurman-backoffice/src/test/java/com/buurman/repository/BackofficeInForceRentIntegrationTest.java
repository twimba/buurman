package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.dto.response.backoffice.dashboard.CountryStats;
import com.buurman.repository.backoffice.BackofficeTeamStatsRepository;
import com.buurman.repository.backoffice.DashboardAggregateRepository;

/**
 * Backoffice rent figures count every in-force contract: a contract under notice ({@code
 * NOTICE_GIVEN}) still generates rent until its effective end date. Lives in {@code
 * com.buurman.repository} to reuse buurman-core's package-private integration test fixtures.
 */
@DisplayName("Backoffice rent aggregates — in-force contracts")
class BackofficeInForceRentIntegrationTest extends AbstractRepositoryIntegrationTest {

  private BackofficeTeamStatsRepository teamStatsRepository;
  private DashboardAggregateRepository dashboardAggregateRepository;

  @BeforeEach
  void setUp() {
    teamStatsRepository = new BackofficeTeamStatsRepository(dsl, mock(UnitRepository.class));
    dashboardAggregateRepository = new DashboardAggregateRepository(dsl, CLOCK);

    insertContractWithStatus(TEAM_A_ID, "NOTICE_GIVEN");
    insertContractWithStatus(TEAM_A_ID, "TERMINATED");
    dsl.insertInto(DSL.table("team_preferences"))
        .set(DSL.field("team_id", UUID.class), TEAM_A_ID)
        .set(DSL.field("default_country_code", String.class), "NL")
        .execute();
  }

  @Test
  @DisplayName("team detail rent total includes a NOTICE_GIVEN contract, not a TERMINATED one")
  void teamRentIncludesNoticeGiven() {
    Map.Entry<BigDecimal, String> rent = teamStatsRepository.sumActiveRentForTeam(TEAM_A_ID);
    assertThat(rent.getKey()).isPositive();
    assertThat(rent.getValue()).isEqualTo("EUR");
    assertThat(teamStatsRepository.sumActiveRentForTeam(TEAM_B_ID).getKey()).isZero();

    terminateNoticeGivenContracts();

    assertThat(teamStatsRepository.sumActiveRentForTeam(TEAM_A_ID).getKey()).isZero();
  }

  @Test
  @DisplayName("per-country monthly value includes a NOTICE_GIVEN contract, not a TERMINATED one")
  void countryMonthlyValueIncludesNoticeGiven() {
    assertThat(nlMonthlyValue()).isPositive();

    terminateNoticeGivenContracts();

    assertThat(nlMonthlyValue()).isZero();
  }

  private long nlMonthlyValue() {
    return dashboardAggregateRepository.countryStats().stream()
        .filter(s -> "NL".equals(s.code()))
        .findFirst()
        .map(CountryStats::monthlyValueEurMinor)
        .orElseThrow();
  }

  private void terminateNoticeGivenContracts() {
    dsl.update(DSL.table("contracts"))
        .set(DSL.field("status", String.class), "TERMINATED")
        .where(DSL.field("status", String.class).eq("NOTICE_GIVEN"))
        .execute();
  }

  private void insertContractWithStatus(UUID teamId, String status) {
    UUID propertyId = TestDataHelper.insertProperty(dsl, teamId, USER_ID);
    UUID contractId = TestDataHelper.insertContract(dsl, teamId, propertyId, USER_ID);
    dsl.update(DSL.table("contracts"))
        .set(DSL.field("status", String.class), status)
        .where(DSL.field("id", UUID.class).eq(contractId))
        .execute();
  }
}
