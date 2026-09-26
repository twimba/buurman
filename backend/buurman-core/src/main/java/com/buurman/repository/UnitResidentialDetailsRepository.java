package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.UNIT_RESIDENTIAL_DETAILS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.UnitResidentialDetails;
import com.buurman.jooq.generated.tables.records.UnitResidentialDetailsRecord;

import lombok.RequiredArgsConstructor;

/**
 * No delete method by design: {@code unit_residential_details} rows are upsert-only and are never
 * removed, even when a unit's type moves away from APARTMENT (see {@code
 * UnitResidentialDetailsService}). The table itself has no {@code deleted_at} column.
 */
@Repository
@RequiredArgsConstructor
public class UnitResidentialDetailsRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public Optional<UnitResidentialDetails> findByUnitIdAndTeamId(UUID unitId, UUID teamId) {
    return dsl.selectFrom(UNIT_RESIDENTIAL_DETAILS)
        .where(
            UNIT_RESIDENTIAL_DETAILS
                .UNIT_ID
                .eq(unitId)
                .and(UNIT_RESIDENTIAL_DETAILS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(this::toDomain);
  }

  public UnitResidentialDetails save(UnitResidentialDetails details) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (details.getId() == null) {
      UUID newId = UUID.randomUUID();
      dsl.insertInto(UNIT_RESIDENTIAL_DETAILS)
          .set(UNIT_RESIDENTIAL_DETAILS.ID, newId)
          .set(UNIT_RESIDENTIAL_DETAILS.UNIT_ID, details.getUnitId())
          .set(UNIT_RESIDENTIAL_DETAILS.TEAM_ID, details.getTeamId())
          .set(UNIT_RESIDENTIAL_DETAILS.BEDROOMS, details.getBedrooms().orElse(null))
          .set(UNIT_RESIDENTIAL_DETAILS.BATHROOMS, details.getBathrooms().orElse(null))
          .set(UNIT_RESIDENTIAL_DETAILS.FURNISHED, details.isFurnished())
          .set(UNIT_RESIDENTIAL_DETAILS.PET_POLICY, details.getPetPolicy().orElse(null))
          .set(UNIT_RESIDENTIAL_DETAILS.CREATED_AT, now)
          .set(UNIT_RESIDENTIAL_DETAILS.UPDATED_AT, now)
          .set(UNIT_RESIDENTIAL_DETAILS.CREATED_BY, details.getCreatedBy().orElse(null))
          .set(UNIT_RESIDENTIAL_DETAILS.UPDATED_BY, details.getUpdatedBy().orElse(null))
          .execute();

      details.setId(newId);
      details.setCreatedAt(Optional.of(now.toInstant(UTC)));
      details.setUpdatedAt(Optional.of(now.toInstant(UTC)));
    } else {
      dsl.update(UNIT_RESIDENTIAL_DETAILS)
          .set(UNIT_RESIDENTIAL_DETAILS.BEDROOMS, details.getBedrooms().orElse(null))
          .set(UNIT_RESIDENTIAL_DETAILS.BATHROOMS, details.getBathrooms().orElse(null))
          .set(UNIT_RESIDENTIAL_DETAILS.FURNISHED, details.isFurnished())
          .set(UNIT_RESIDENTIAL_DETAILS.PET_POLICY, details.getPetPolicy().orElse(null))
          .set(UNIT_RESIDENTIAL_DETAILS.UPDATED_AT, now)
          .set(UNIT_RESIDENTIAL_DETAILS.UPDATED_BY, details.getUpdatedBy().orElse(null))
          .where(
              UNIT_RESIDENTIAL_DETAILS
                  .ID
                  .eq(details.getId())
                  .and(UNIT_RESIDENTIAL_DETAILS.TEAM_ID.eq(details.getTeamId())))
          .execute();

      details.setUpdatedAt(Optional.of(now.toInstant(UTC)));
    }

    return details;
  }

  private UnitResidentialDetails toDomain(UnitResidentialDetailsRecord record) {
    UnitResidentialDetails details = new UnitResidentialDetails();
    details.setId(record.getId());
    details.setUnitId(record.getUnitId());
    details.setTeamId(record.getTeamId());
    details.setBedrooms(Optional.ofNullable(record.getBedrooms()));
    details.setBathrooms(Optional.ofNullable(record.getBathrooms()));
    details.setFurnished(Boolean.TRUE.equals(record.getFurnished()));
    details.setPetPolicy(Optional.ofNullable(record.getPetPolicy()));
    details.setCreatedAt(Optional.ofNullable(record.getCreatedAt()).map(dt -> dt.toInstant(UTC)));
    details.setUpdatedAt(Optional.ofNullable(record.getUpdatedAt()).map(dt -> dt.toInstant(UTC)));
    details.setCreatedBy(Optional.ofNullable(record.getCreatedBy()));
    details.setUpdatedBy(Optional.ofNullable(record.getUpdatedBy()));
    return details;
  }
}
