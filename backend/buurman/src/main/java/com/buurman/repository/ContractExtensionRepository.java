package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTRACT_EXTENSIONS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContractExtension;
import com.buurman.domain.Sid;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.ContractExtensionRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContractExtensionRepository {

  private final DSLContext dsl;
  private final ContractExtensionRecordMapper mapper;
  private final Clock clock;

  public ContractExtension save(ContractExtension extension) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (extension.getId() == null) {
      UUID id = UUID.randomUUID();

      dsl.insertInto(CONTRACT_EXTENSIONS)
          .set(CONTRACT_EXTENSIONS.ID, id)
          .set(CONTRACT_EXTENSIONS.IDENTIFIER, extension.getIdentifier().orElseThrow())
          .set(CONTRACT_EXTENSIONS.TEAM_ID, extension.getTeamId())
          .set(CONTRACT_EXTENSIONS.CONTRACT_ID, extension.getContractId())
          .set(CONTRACT_EXTENSIONS.EXTENSION_NUMBER, extension.getExtensionNumber())
          .set(CONTRACT_EXTENSIONS.PREVIOUS_END_DATE, extension.getPreviousEndDate())
          .set(CONTRACT_EXTENSIONS.NEW_END_DATE, extension.getNewEndDate().orElse(null))
          .set(
              CONTRACT_EXTENSIONS.PREVIOUS_RENT_AMOUNT,
              extension.getPreviousRentAmount().toMinorUnits())
          .set(
              CONTRACT_EXTENSIONS.PREVIOUS_RENT_CURRENCY,
              extension.getPreviousRentAmount().currency())
          .set(CONTRACT_EXTENSIONS.NEW_RENT_AMOUNT, extension.getNewRentAmount().toMinorUnits())
          .set(CONTRACT_EXTENSIONS.NEW_RENT_CURRENCY, extension.getNewRentAmount().currency())
          .set(CONTRACT_EXTENSIONS.RENT_ADJUSTMENT_TYPE, extension.getRentAdjustmentType().name())
          .set(
              CONTRACT_EXTENSIONS.RENT_ADJUSTMENT_VALUE,
              extension.getRentAdjustmentValue().orElse(null))
          .set(CONTRACT_EXTENSIONS.STATUS, extension.getStatus().name())
          .set(CONTRACT_EXTENSIONS.TRIGGER_TYPE, extension.getTriggerType().name())
          .set(CONTRACT_EXTENSIONS.RENT_PERIOD_ID, extension.getRentPeriodId().orElse(null))
          .set(CONTRACT_EXTENSIONS.NOTES, extension.getNotes().orElse(null))
          .set(CONTRACT_EXTENSIONS.DECLINED_REASON, extension.getDeclinedReason().orElse(null))
          .set(
              CONTRACT_EXTENSIONS.ACTIVATED_AT,
              extension.getActivatedAt().map(i -> i.atZone(UTC).toLocalDateTime()).orElse(null))
          .set(CONTRACT_EXTENSIONS.ACTIVATED_BY, extension.getActivatedBy().orElse(null))
          .set(
              CONTRACT_EXTENSIONS.CONFIRMED_AT,
              extension.getConfirmedAt().map(i -> i.atZone(UTC).toLocalDateTime()).orElse(null))
          .set(CONTRACT_EXTENSIONS.CONFIRMED_BY, extension.getConfirmedBy().orElse(null))
          .set(
              CONTRACT_EXTENSIONS.SUPERSEDED_AT,
              extension.getSupersededAt().map(i -> i.atZone(UTC).toLocalDateTime()).orElse(null))
          .set(CONTRACT_EXTENSIONS.CREATED_AT, now)
          .set(CONTRACT_EXTENSIONS.UPDATED_AT, now)
          .set(CONTRACT_EXTENSIONS.CREATED_BY, extension.getCreatedBy())
          .set(CONTRACT_EXTENSIONS.UPDATED_BY, extension.getUpdatedBy())
          .execute();

      extension.setId(id);
      extension.setCreatedAt(now.toInstant(UTC));
      extension.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(CONTRACT_EXTENSIONS)
          .set(CONTRACT_EXTENSIONS.NEW_END_DATE, extension.getNewEndDate().orElse(null))
          .set(CONTRACT_EXTENSIONS.NEW_RENT_AMOUNT, extension.getNewRentAmount().toMinorUnits())
          .set(CONTRACT_EXTENSIONS.NEW_RENT_CURRENCY, extension.getNewRentAmount().currency())
          .set(CONTRACT_EXTENSIONS.RENT_ADJUSTMENT_TYPE, extension.getRentAdjustmentType().name())
          .set(
              CONTRACT_EXTENSIONS.RENT_ADJUSTMENT_VALUE,
              extension.getRentAdjustmentValue().orElse(null))
          .set(CONTRACT_EXTENSIONS.STATUS, extension.getStatus().name())
          .set(CONTRACT_EXTENSIONS.RENT_PERIOD_ID, extension.getRentPeriodId().orElse(null))
          .set(CONTRACT_EXTENSIONS.NOTES, extension.getNotes().orElse(null))
          .set(CONTRACT_EXTENSIONS.DECLINED_REASON, extension.getDeclinedReason().orElse(null))
          .set(
              CONTRACT_EXTENSIONS.ACTIVATED_AT,
              extension.getActivatedAt().map(i -> i.atZone(UTC).toLocalDateTime()).orElse(null))
          .set(CONTRACT_EXTENSIONS.ACTIVATED_BY, extension.getActivatedBy().orElse(null))
          .set(
              CONTRACT_EXTENSIONS.CONFIRMED_AT,
              extension.getConfirmedAt().map(i -> i.atZone(UTC).toLocalDateTime()).orElse(null))
          .set(CONTRACT_EXTENSIONS.CONFIRMED_BY, extension.getConfirmedBy().orElse(null))
          .set(
              CONTRACT_EXTENSIONS.SUPERSEDED_AT,
              extension.getSupersededAt().map(i -> i.atZone(UTC).toLocalDateTime()).orElse(null))
          .set(CONTRACT_EXTENSIONS.UPDATED_AT, now)
          .set(CONTRACT_EXTENSIONS.UPDATED_BY, extension.getUpdatedBy())
          .set(
              CONTRACT_EXTENSIONS.DELETED_AT,
              extension.getDeletedAt().map(i -> i.atZone(UTC).toLocalDateTime()).orElse(null))
          .where(
              CONTRACT_EXTENSIONS
                  .ID
                  .eq(extension.getId())
                  .and(CONTRACT_EXTENSIONS.TEAM_ID.eq(extension.getTeamId())))
          .execute();

      extension.setUpdatedAt(now.toInstant(UTC));
    }
    return extension;
  }

  public Optional<ContractExtension> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CONTRACT_EXTENSIONS)
        .where(
            CONTRACT_EXTENSIONS
                .IDENTIFIER
                .eq(identifier)
                .and(CONTRACT_EXTENSIONS.TEAM_ID.eq(teamId))
                .and(CONTRACT_EXTENSIONS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public ContractExtension getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Contract extension not found"));
  }

  public List<ContractExtension> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return dsl.selectFrom(CONTRACT_EXTENSIONS)
        .where(
            CONTRACT_EXTENSIONS
                .CONTRACT_ID
                .eq(contractId)
                .and(CONTRACT_EXTENSIONS.TEAM_ID.eq(teamId))
                .and(CONTRACT_EXTENSIONS.DELETED_AT.isNull()))
        .orderBy(CONTRACT_EXTENSIONS.EXTENSION_NUMBER.desc())
        .fetch()
        .map(mapper::toDomain);
  }

  public PaginatedResult<ContractExtension> findByContractIdPaginated(
      UUID contractId, UUID teamId, PageRequest pageRequest) {
    Condition condition =
        CONTRACT_EXTENSIONS
            .CONTRACT_ID
            .eq(contractId)
            .and(CONTRACT_EXTENSIONS.TEAM_ID.eq(teamId))
            .and(CONTRACT_EXTENSIONS.DELETED_AT.isNull());

    java.util.Map<String, Field<?>> sortableFields =
        java.util.Map.of(
            "extensionNumber", CONTRACT_EXTENSIONS.EXTENSION_NUMBER,
            "createdAt", CONTRACT_EXTENSIONS.CREATED_AT,
            "status", CONTRACT_EXTENSIONS.STATUS);

    return PaginationHelper.paginate(
        dsl,
        CONTRACT_EXTENSIONS,
        condition,
        sortableFields,
        CONTRACT_EXTENSIONS.EXTENSION_NUMBER,
        pageRequest,
        mapper::toDomain);
  }

  public Optional<ContractExtension> findActiveByContractId(UUID contractId, UUID teamId) {
    return dsl.selectFrom(CONTRACT_EXTENSIONS)
        .where(
            CONTRACT_EXTENSIONS
                .CONTRACT_ID
                .eq(contractId)
                .and(CONTRACT_EXTENSIONS.TEAM_ID.eq(teamId))
                .and(CONTRACT_EXTENSIONS.STATUS.eq("ACTIVE"))
                .and(CONTRACT_EXTENSIONS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Optional<ContractExtension> findDraftByContractId(UUID contractId, UUID teamId) {
    return dsl.selectFrom(CONTRACT_EXTENSIONS)
        .where(
            CONTRACT_EXTENSIONS
                .CONTRACT_ID
                .eq(contractId)
                .and(CONTRACT_EXTENSIONS.TEAM_ID.eq(teamId))
                .and(CONTRACT_EXTENSIONS.STATUS.eq("DRAFT"))
                .and(CONTRACT_EXTENSIONS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public int getNextExtensionNumber(UUID contractId) {
    Integer max =
        dsl.select(DSL.max(CONTRACT_EXTENSIONS.EXTENSION_NUMBER))
            .from(CONTRACT_EXTENSIONS)
            .where(CONTRACT_EXTENSIONS.CONTRACT_ID.eq(contractId))
            .fetchOne(DSL.max(CONTRACT_EXTENSIONS.EXTENSION_NUMBER));
    return (max == null) ? 1 : max + 1;
  }

  public int countActiveAndSuperseded(UUID contractId, UUID teamId) {
    return dsl.fetchCount(
        CONTRACT_EXTENSIONS,
        CONTRACT_EXTENSIONS
            .CONTRACT_ID
            .eq(contractId)
            .and(CONTRACT_EXTENSIONS.TEAM_ID.eq(teamId))
            .and(CONTRACT_EXTENSIONS.STATUS.in("ACTIVE", "SUPERSEDED"))
            .and(CONTRACT_EXTENSIONS.DELETED_AT.isNull()));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTRACT_EXTENSIONS)
        .set(CONTRACT_EXTENSIONS.DELETED_AT, now)
        .set(CONTRACT_EXTENSIONS.STATUS, "CANCELLED")
        .where(CONTRACT_EXTENSIONS.ID.eq(id).and(CONTRACT_EXTENSIONS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
