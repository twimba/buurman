package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTRACT_LEASE_CLAUSES;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContractLeaseClause;
import com.buurman.mapper.ContractLeaseClauseRecordMapper;

import lombok.RequiredArgsConstructor;

/** Per-contract clause overrides — team-scoped on every read and write. */
@Repository
@RequiredArgsConstructor
public class ContractLeaseClauseRepository {

  private final DSLContext dsl;
  private final ContractLeaseClauseRecordMapper mapper;
  private final Clock clock;

  public List<ContractLeaseClause> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(CONTRACT_LEASE_CLAUSES)
            .where(
                CONTRACT_LEASE_CLAUSES
                    .CONTRACT_ID
                    .eq(contractId)
                    .and(CONTRACT_LEASE_CLAUSES.TEAM_ID.eq(teamId)))
            .fetch()
            .map(mapper::toDomain));
  }

  /**
   * Replaces all per-contract overrides in one call — the PUT endpoint always sends the full set.
   */
  public void replaceForContract(
      UUID contractId, UUID teamId, UUID actorId, List<ContractLeaseClause> clauses) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.deleteFrom(CONTRACT_LEASE_CLAUSES)
        .where(
            CONTRACT_LEASE_CLAUSES
                .CONTRACT_ID
                .eq(contractId)
                .and(CONTRACT_LEASE_CLAUSES.TEAM_ID.eq(teamId)))
        .execute();

    for (ContractLeaseClause clause : clauses) {
      dsl.insertInto(CONTRACT_LEASE_CLAUSES)
          .set(CONTRACT_LEASE_CLAUSES.ID, UUID.randomUUID())
          .set(CONTRACT_LEASE_CLAUSES.TEAM_ID, teamId)
          .set(CONTRACT_LEASE_CLAUSES.CONTRACT_ID, contractId)
          .set(CONTRACT_LEASE_CLAUSES.CLAUSE_TEMPLATE_ID, clause.getClauseTemplateId())
          .set(CONTRACT_LEASE_CLAUSES.INCLUDED, clause.isIncluded())
          .set(CONTRACT_LEASE_CLAUSES.SORT_ORDER, clause.getSortOrder())
          .set(CONTRACT_LEASE_CLAUSES.CREATED_AT, now)
          .set(CONTRACT_LEASE_CLAUSES.UPDATED_AT, now)
          .set(CONTRACT_LEASE_CLAUSES.CREATED_BY, actorId)
          .set(CONTRACT_LEASE_CLAUSES.UPDATED_BY, actorId)
          .execute();
    }
  }
}
