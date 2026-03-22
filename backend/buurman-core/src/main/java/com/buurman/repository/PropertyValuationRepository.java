package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_VALUATIONS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyValuation;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PropertyValuationRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyValuationRepository {

  private final DSLContext dsl;
  private final PropertyValuationRecordMapper mapper;
  private final Clock clock;

  public Optional<PropertyValuation> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(PROPERTY_VALUATIONS)
        .where(
            PROPERTY_VALUATIONS
                .IDENTIFIER
                .eq(identifier)
                .and(PROPERTY_VALUATIONS.TEAM_ID.eq(teamId))
                .and(PROPERTY_VALUATIONS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public PropertyValuation getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Property valuation not found"));
  }

  public List<PropertyValuation> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_VALUATIONS)
        .where(
            PROPERTY_VALUATIONS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_VALUATIONS.TEAM_ID.eq(teamId))
                .and(PROPERTY_VALUATIONS.DELETED_AT.isNull()))
        .orderBy(PROPERTY_VALUATIONS.VALUATION_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public Optional<PropertyValuation> findLatestByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl.selectFrom(PROPERTY_VALUATIONS)
        .where(
            PROPERTY_VALUATIONS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_VALUATIONS.TEAM_ID.eq(teamId))
                .and(PROPERTY_VALUATIONS.DELETED_AT.isNull()))
        .orderBy(PROPERTY_VALUATIONS.VALUATION_DATE.desc())
        .limit(1)
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public PropertyValuation save(PropertyValuation val) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (val.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          val.getCreatedAt() != null ? LocalDateTime.ofInstant(val.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          val.getUpdatedAt() != null ? LocalDateTime.ofInstant(val.getUpdatedAt(), UTC) : now;

      dsl.insertInto(PROPERTY_VALUATIONS)
          .set(PROPERTY_VALUATIONS.ID, id)
          .set(PROPERTY_VALUATIONS.IDENTIFIER, val.getIdentifier().orElseThrow())
          .set(PROPERTY_VALUATIONS.PROPERTY_ID, val.getPropertyId())
          .set(PROPERTY_VALUATIONS.TEAM_ID, val.getTeamId())
          .set(PROPERTY_VALUATIONS.VALUATION_TYPE, val.getValuationType().name())
          .set(PROPERTY_VALUATIONS.VALUATION_DATE, val.getValuationDate())
          .set(PROPERTY_VALUATIONS.AMOUNT, val.getAmount().value())
          .set(PROPERTY_VALUATIONS.CURRENCY, val.getAmount().currency())
          .set(PROPERTY_VALUATIONS.SOURCE, val.getSource().orElse(null))
          .set(PROPERTY_VALUATIONS.NOTES, val.getNotes().orElse(null))
          .set(PROPERTY_VALUATIONS.CREATED_AT, createdAt)
          .set(PROPERTY_VALUATIONS.UPDATED_AT, updatedAt)
          .set(PROPERTY_VALUATIONS.CREATED_BY, val.getCreatedBy())
          .set(PROPERTY_VALUATIONS.UPDATED_BY, val.getUpdatedBy())
          .execute();

      val.setId(id);
      val.setCreatedAt(createdAt.toInstant(UTC));
      val.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // Update
      LocalDateTime updatedAt =
          val.getUpdatedAt() != null ? LocalDateTime.ofInstant(val.getUpdatedAt(), UTC) : now;

      dsl.update(PROPERTY_VALUATIONS)
          .set(PROPERTY_VALUATIONS.VALUATION_TYPE, val.getValuationType().name())
          .set(PROPERTY_VALUATIONS.VALUATION_DATE, val.getValuationDate())
          .set(PROPERTY_VALUATIONS.AMOUNT, val.getAmount().value())
          .set(PROPERTY_VALUATIONS.CURRENCY, val.getAmount().currency())
          .set(PROPERTY_VALUATIONS.SOURCE, val.getSource().orElse(null))
          .set(PROPERTY_VALUATIONS.NOTES, val.getNotes().orElse(null))
          .set(PROPERTY_VALUATIONS.UPDATED_AT, updatedAt)
          .set(PROPERTY_VALUATIONS.UPDATED_BY, val.getUpdatedBy())
          .where(
              PROPERTY_VALUATIONS
                  .ID
                  .eq(val.getId())
                  .and(PROPERTY_VALUATIONS.TEAM_ID.eq(val.getTeamId())))
          .execute();

      val.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return val;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTY_VALUATIONS)
        .set(PROPERTY_VALUATIONS.DELETED_AT, now)
        .where(PROPERTY_VALUATIONS.ID.eq(id).and(PROPERTY_VALUATIONS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
