package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTRACT_TERMINATIONS;
import static com.buurman.util.SidGenerator.newContractTerminationId;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContractTermination;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.ContractTerminationRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContractTerminationRepository {

  private final DSLContext dsl;
  private final ContractTerminationRecordMapper mapper;
  private final Clock clock;

  public ContractTermination save(ContractTermination termination) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (termination.getId() == null) {
      UUID newId = UUID.randomUUID();
      Sid identifier = newContractTerminationId();

      dsl.insertInto(CONTRACT_TERMINATIONS)
          .set(CONTRACT_TERMINATIONS.ID, newId)
          .set(CONTRACT_TERMINATIONS.IDENTIFIER, identifier)
          .set(CONTRACT_TERMINATIONS.TEAM_ID, termination.getTeamId())
          .set(CONTRACT_TERMINATIONS.CONTRACT_ID, termination.getContractId())
          .set(CONTRACT_TERMINATIONS.GIVEN_BY, termination.getGivenBy().name())
          .set(CONTRACT_TERMINATIONS.NOTICE_DATE, termination.getNoticeDate())
          .set(CONTRACT_TERMINATIONS.GROUND_CODE, termination.getGroundCode().orElse(null))
          .set(CONTRACT_TERMINATIONS.COMPUTED_END_DATE, termination.getComputedEndDate())
          .set(CONTRACT_TERMINATIONS.EFFECTIVE_END_DATE, termination.getEffectiveEndDate())
          .set(CONTRACT_TERMINATIONS.OVERRIDE_REASON, termination.getOverrideReason().orElse(null))
          .set(CONTRACT_TERMINATIONS.INSPECTION_DATE, termination.getInspectionDate().orElse(null))
          .set(
              CONTRACT_TERMINATIONS.NOTICE_LETTER_DOCUMENT_ID,
              termination.getNoticeLetterDocumentId().orElse(null))
          .set(CONTRACT_TERMINATIONS.STATUS, termination.getStatus().name())
          .set(CONTRACT_TERMINATIONS.CREATED_AT, now)
          .set(CONTRACT_TERMINATIONS.UPDATED_AT, now)
          .set(CONTRACT_TERMINATIONS.CREATED_BY, termination.getCreatedBy())
          .set(CONTRACT_TERMINATIONS.UPDATED_BY, termination.getUpdatedBy())
          .execute();

      termination.setId(newId);
      termination.setIdentifier(Optional.of(identifier));
      termination.setCreatedAt(now.toInstant(UTC));
      termination.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(CONTRACT_TERMINATIONS)
          .set(CONTRACT_TERMINATIONS.STATUS, termination.getStatus().name())
          .set(
              CONTRACT_TERMINATIONS.NOTICE_LETTER_DOCUMENT_ID,
              termination.getNoticeLetterDocumentId().orElse(null))
          .set(CONTRACT_TERMINATIONS.UPDATED_AT, now)
          .set(CONTRACT_TERMINATIONS.UPDATED_BY, termination.getUpdatedBy())
          .where(
              CONTRACT_TERMINATIONS
                  .ID
                  .eq(termination.getId())
                  .and(CONTRACT_TERMINATIONS.TEAM_ID.eq(termination.getTeamId())))
          .execute();
      termination.setUpdatedAt(now.toInstant(UTC));
    }

    return termination;
  }

  public Optional<ContractTermination> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CONTRACT_TERMINATIONS)
        .where(
            CONTRACT_TERMINATIONS
                .IDENTIFIER
                .eq(identifier)
                .and(CONTRACT_TERMINATIONS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public ContractTermination getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Contract termination not found"));
  }

  public Optional<ContractTermination> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return dsl.selectFrom(CONTRACT_TERMINATIONS)
        .where(
            CONTRACT_TERMINATIONS
                .CONTRACT_ID
                .eq(contractId)
                .and(CONTRACT_TERMINATIONS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  /** Team-agnostic: used only by the daily sweep job, which has no authenticated team context. */
  public List<ContractTermination> findDueForTransition(LocalDate onOrBefore) {
    return List.copyOf(
        dsl.selectFrom(CONTRACT_TERMINATIONS)
            .where(
                CONTRACT_TERMINATIONS
                    .STATUS
                    .eq("NOTICE_GIVEN")
                    .and(CONTRACT_TERMINATIONS.EFFECTIVE_END_DATE.le(onOrBefore)))
            .fetch()
            .map(mapper::toDomain));
  }
}
