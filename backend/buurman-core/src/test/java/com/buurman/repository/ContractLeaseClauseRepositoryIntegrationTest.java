package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.mapper.ContractLeaseClauseRecordMapperImpl;
import com.buurman.mapper.LeaseClauseTemplateRecordMapperImpl;

@DisplayName("ContractLeaseClauseRepository")
class ContractLeaseClauseRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContractLeaseClauseRepository repository;
  private UUID teamAContractId;
  private UUID teamBContractId;
  private UUID templateId;

  @BeforeEach
  void setUp() {
    repository =
        new ContractLeaseClauseRepository(dsl, new ContractLeaseClauseRecordMapperImpl(), CLOCK);
    LeaseClauseTemplateRepository templateRepository =
        new LeaseClauseTemplateRepository(dsl, new LeaseClauseTemplateRecordMapperImpl(), CLOCK);

    UUID teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID teamBPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    teamAContractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, teamAPropertyId, USER_ID);
    teamBContractId = TestDataHelper.insertContract(dsl, TEAM_B_ID, teamBPropertyId, USER_ID);

    LeaseClauseTemplate template =
        templateRepository.save(
            LeaseClauseTemplate.builder()
                .countryCode("NL")
                .clauseKey("house-rules")
                .titleI18nKey("lease.house-rules.title")
                .bodyI18nKey("lease.house-rules.body")
                .defaultIncluded(true)
                .optional(true)
                .sortOrder(5)
                .version(1)
                .build());
    templateId = template.getId();
  }

  @Test
  @DisplayName(
      "replaceForContract is team-scoped — team B never sees or affects team A's overrides")
  void teamIsolation() {
    repository.replaceForContract(
        teamAContractId,
        TEAM_A_ID,
        USER_ID,
        List.of(
            ContractLeaseClause.builder()
                .clauseTemplateId(templateId)
                .included(false)
                .sortOrder(5)
                .build()));

    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_A_ID)).hasSize(1);
    assertThat(repository.findByContractIdAndTeamId(teamBContractId, TEAM_B_ID)).isEmpty();
    // A team-B lookup using team A's contract id (wrong team) also finds nothing.
    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_B_ID)).isEmpty();
  }

  @Test
  @DisplayName("replaceForContract deletes the previous set before inserting the new one")
  void replaceWholesale() {
    repository.replaceForContract(
        teamAContractId,
        TEAM_A_ID,
        USER_ID,
        List.of(
            ContractLeaseClause.builder()
                .clauseTemplateId(templateId)
                .included(false)
                .sortOrder(5)
                .build()));
    repository.replaceForContract(teamAContractId, TEAM_A_ID, USER_ID, List.of());

    assertThat(repository.findByContractIdAndTeamId(teamAContractId, TEAM_A_ID)).isEmpty();
  }
}
