package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.WWS_CALCULATIONS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Sid;
import com.buurman.domain.WwsCalculation;
import com.buurman.mapper.WwsCalculationRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WwsCalculationRepository {

  private final DSLContext dsl;
  private final WwsCalculationRecordMapper mapper;
  private final Clock clock;

  public WwsCalculation save(WwsCalculation calc) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID id = UUID.randomUUID();

    dsl.insertInto(WWS_CALCULATIONS)
        .set(WWS_CALCULATIONS.ID, id)
        .set(WWS_CALCULATIONS.IDENTIFIER, calc.getIdentifier().orElseThrow())
        .set(WWS_CALCULATIONS.TEAM_ID, calc.getTeamId())
        .set(WWS_CALCULATIONS.PROPERTY_ID, calc.getPropertyId())
        .set(WWS_CALCULATIONS.CONTRACT_ID, calc.getContractId().orElse(null))
        .set(WWS_CALCULATIONS.SYSTEM_VERSION, calc.getSystemVersion())
        .set(WWS_CALCULATIONS.TOTAL_POINTS, calc.getTotalPoints())
        .set(WWS_CALCULATIONS.SECTOR_CLASSIFICATION, calc.getSectorClassification())
        .set(WWS_CALCULATIONS.MAX_RENT_INDICATION, calc.getMaxRentIndication().orElse(null))
        .set(WWS_CALCULATIONS.CATEGORY_BREAKDOWN, JSONB.jsonb(calc.getBreakdownJson()))
        .set(WWS_CALCULATIONS.INPUT_DATA, JSONB.jsonb(calc.getInputDataJson()))
        .set(WWS_CALCULATIONS.CALCULATION_DATE, calc.getCalculationDate())
        .set(WWS_CALCULATIONS.NOTES, calc.getNotes().orElse(null))
        .set(WWS_CALCULATIONS.CREATED_AT, now)
        .set(WWS_CALCULATIONS.UPDATED_AT, now)
        .set(WWS_CALCULATIONS.CREATED_BY, calc.getCreatedBy())
        .set(WWS_CALCULATIONS.UPDATED_BY, calc.getUpdatedBy())
        .execute();

    calc.setId(id);
    calc.setCreatedAt(now.toInstant(UTC));
    calc.setUpdatedAt(now.toInstant(UTC));
    return calc;
  }

  public List<WwsCalculation> findByPropertyId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(WWS_CALCULATIONS)
        .where(
            WWS_CALCULATIONS
                .PROPERTY_ID
                .eq(propertyId)
                .and(WWS_CALCULATIONS.TEAM_ID.eq(teamId))
                .and(WWS_CALCULATIONS.DELETED_AT.isNull()))
        .orderBy(WWS_CALCULATIONS.CALCULATION_DATE.desc(), WWS_CALCULATIONS.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public Optional<WwsCalculation> findLatestByPropertyId(UUID propertyId, UUID teamId) {
    return dsl.selectFrom(WWS_CALCULATIONS)
        .where(
            WWS_CALCULATIONS
                .PROPERTY_ID
                .eq(propertyId)
                .and(WWS_CALCULATIONS.TEAM_ID.eq(teamId))
                .and(WWS_CALCULATIONS.DELETED_AT.isNull()))
        .orderBy(WWS_CALCULATIONS.CALCULATION_DATE.desc(), WWS_CALCULATIONS.CREATED_AT.desc())
        .limit(1)
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public Optional<WwsCalculation> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(WWS_CALCULATIONS)
        .where(
            WWS_CALCULATIONS
                .IDENTIFIER
                .eq(identifier)
                .and(WWS_CALCULATIONS.TEAM_ID.eq(teamId))
                .and(WWS_CALCULATIONS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public boolean softDelete(Sid identifier, UUID teamId, UUID deletedBy) {
    LocalDateTime now = LocalDateTime.now(clock);
    return dsl.update(WWS_CALCULATIONS)
            .set(WWS_CALCULATIONS.DELETED_AT, now)
            .set(WWS_CALCULATIONS.UPDATED_AT, now)
            .set(WWS_CALCULATIONS.UPDATED_BY, deletedBy)
            .where(
                WWS_CALCULATIONS
                    .IDENTIFIER
                    .eq(identifier)
                    .and(WWS_CALCULATIONS.TEAM_ID.eq(teamId))
                    .and(WWS_CALCULATIONS.DELETED_AT.isNull()))
            .execute()
        > 0;
  }
}
