package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContractTermination;
import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.mapper.ContractTerminationRecordMapperImpl;

@DisplayName("ContractTerminationRepository")
class ContractTerminationRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContractTerminationRepository repository;
  private UUID teamAContractId;
  private UUID teamBContractId;

  @BeforeEach
  void setUp() {
    repository =
        new ContractTerminationRepository(dsl, new ContractTerminationRecordMapperImpl(), CLOCK);
    UUID teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID teamBPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    teamAContractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, teamAPropertyId, USER_ID);
    teamBContractId = TestDataHelper.insertContract(dsl, TEAM_B_ID, teamBPropertyId, USER_ID);
  }

  private ContractTermination newTermination(UUID teamId, UUID contractId) {
    return ContractTermination.builder()
        .teamId(teamId)
        .contractId(contractId)
        .givenBy(TerminationGivenBy.LANDLORD)
        .noticeDate(LocalDate.of(2026, 1, 1))
        .computedEndDate(LocalDate.of(2026, 4, 1))
        .effectiveEndDate(LocalDate.of(2026, 4, 1))
        .status(ContractTerminationStatus.NOTICE_GIVEN)
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  @Test
  @DisplayName(
      "saves and re-reads a termination, and a team-B lookup by a team-A identifier finds nothing")
  void teamIsolation() {
    ContractTermination saved = repository.save(newTermination(TEAM_A_ID, teamAContractId));
    assertThat(saved.getIdentifier()).isPresent();

    assertThat(repository.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID))
        .isPresent();
    assertThat(repository.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID))
        .isEmpty();
  }

  @Test
  @DisplayName(
      "findDueForTransition finds NOTICE_GIVEN terminations on/before the given date, ignores later"
          + " ones")
  void findsDueForTransition() {
    ContractTermination due =
        newTermination(TEAM_A_ID, teamAContractId).toBuilder()
            .effectiveEndDate(LocalDate.of(2026, 1, 10))
            .build();
    repository.save(due);
    ContractTermination notDue =
        newTermination(TEAM_B_ID, teamBContractId).toBuilder()
            .effectiveEndDate(LocalDate.of(2026, 12, 31))
            .build();
    repository.save(notDue);

    var results = repository.findDueForTransition(LocalDate.of(2026, 1, 15));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getContractId()).isEqualTo(teamAContractId);
  }

  @Test
  @DisplayName("findByContractIdAndTeamId is team-scoped")
  void findByContractIsTeamScoped() {
    repository.save(newTermination(TEAM_A_ID, teamAContractId));

    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_A_ID)).isPresent();
    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_B_ID)).isEmpty();
  }
}
