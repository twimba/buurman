package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_OCCUPANCY_PERIODS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyOccupancyPeriod;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PropertyOccupancyPeriodRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyOccupancyPeriodRepository {

  private final DSLContext dsl;
  private final PropertyOccupancyPeriodRecordMapper mapper;
  private final Clock clock;

  public Optional<PropertyOccupancyPeriod> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(PROPERTY_OCCUPANCY_PERIODS)
        .where(
            PROPERTY_OCCUPANCY_PERIODS
                .IDENTIFIER
                .eq(identifier)
                .and(PROPERTY_OCCUPANCY_PERIODS.TEAM_ID.eq(teamId))
                .and(PROPERTY_OCCUPANCY_PERIODS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public PropertyOccupancyPeriod getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Occupancy period not found"));
  }

  public List<PropertyOccupancyPeriod> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_OCCUPANCY_PERIODS)
        .where(
            PROPERTY_OCCUPANCY_PERIODS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_OCCUPANCY_PERIODS.TEAM_ID.eq(teamId))
                .and(PROPERTY_OCCUPANCY_PERIODS.DELETED_AT.isNull()))
        .orderBy(PROPERTY_OCCUPANCY_PERIODS.START_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public Optional<PropertyOccupancyPeriod> findActiveByPropertyIdAndTeamId(
      UUID propertyId, UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    return dsl.selectFrom(PROPERTY_OCCUPANCY_PERIODS)
        .where(
            PROPERTY_OCCUPANCY_PERIODS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_OCCUPANCY_PERIODS.TEAM_ID.eq(teamId))
                .and(PROPERTY_OCCUPANCY_PERIODS.DELETED_AT.isNull())
                .and(PROPERTY_OCCUPANCY_PERIODS.START_DATE.le(today))
                .and(
                    PROPERTY_OCCUPANCY_PERIODS
                        .END_DATE
                        .isNull()
                        .or(PROPERTY_OCCUPANCY_PERIODS.END_DATE.ge(today))))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  /**
   * Find overlapping periods for a property, optionally excluding a specific period (for updates).
   */
  public List<PropertyOccupancyPeriod> findOverlapping(
      UUID propertyId,
      UUID teamId,
      LocalDate startDate,
      LocalDate endDate,
      @Nullable UUID excludeId) {
    Condition condition =
        PROPERTY_OCCUPANCY_PERIODS
            .PROPERTY_ID
            .eq(propertyId)
            .and(PROPERTY_OCCUPANCY_PERIODS.TEAM_ID.eq(teamId))
            .and(PROPERTY_OCCUPANCY_PERIODS.DELETED_AT.isNull())
            .and(PROPERTY_OCCUPANCY_PERIODS.START_DATE.le(endDate))
            .and(
                PROPERTY_OCCUPANCY_PERIODS
                    .END_DATE
                    .isNull()
                    .or(PROPERTY_OCCUPANCY_PERIODS.END_DATE.ge(startDate)));

    if (excludeId != null) {
      condition = condition.and(PROPERTY_OCCUPANCY_PERIODS.ID.ne(excludeId));
    }

    return dsl.selectFrom(PROPERTY_OCCUPANCY_PERIODS).where(condition).fetch().stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<PropertyOccupancyPeriod> findAllByTeamId(UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_OCCUPANCY_PERIODS)
        .where(
            PROPERTY_OCCUPANCY_PERIODS
                .TEAM_ID
                .eq(teamId)
                .and(PROPERTY_OCCUPANCY_PERIODS.DELETED_AT.isNull()))
        .orderBy(PROPERTY_OCCUPANCY_PERIODS.START_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public PropertyOccupancyPeriod save(PropertyOccupancyPeriod period) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (period.getId() == null) {
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          period.getCreatedAt() != null ? LocalDateTime.ofInstant(period.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          period.getUpdatedAt() != null ? LocalDateTime.ofInstant(period.getUpdatedAt(), UTC) : now;

      dsl.insertInto(PROPERTY_OCCUPANCY_PERIODS)
          .set(PROPERTY_OCCUPANCY_PERIODS.ID, id)
          .set(PROPERTY_OCCUPANCY_PERIODS.IDENTIFIER, period.getIdentifier().orElseThrow())
          .set(PROPERTY_OCCUPANCY_PERIODS.TEAM_ID, period.getTeamId())
          .set(PROPERTY_OCCUPANCY_PERIODS.PROPERTY_ID, period.getPropertyId())
          .set(PROPERTY_OCCUPANCY_PERIODS.START_DATE, period.getStartDate())
          .set(PROPERTY_OCCUPANCY_PERIODS.END_DATE, period.getEndDate().orElse(null))
          .set(PROPERTY_OCCUPANCY_PERIODS.TYPE, period.getType().name())
          .set(PROPERTY_OCCUPANCY_PERIODS.OCCUPANT_NAME, period.getOccupantName().orElse(null))
          .set(
              PROPERTY_OCCUPANCY_PERIODS.MONTHLY_IMPUTED_RENT,
              period.getMonthlyImputedRent().orElse(null))
          .set(
              PROPERTY_OCCUPANCY_PERIODS.END_REASON,
              period.getEndReason().map(Enum::name).orElse(null))
          .set(PROPERTY_OCCUPANCY_PERIODS.NOTES, period.getNotes().orElse(null))
          .set(PROPERTY_OCCUPANCY_PERIODS.CREATED_AT, createdAt)
          .set(PROPERTY_OCCUPANCY_PERIODS.UPDATED_AT, updatedAt)
          .set(PROPERTY_OCCUPANCY_PERIODS.CREATED_BY, period.getCreatedBy())
          .set(PROPERTY_OCCUPANCY_PERIODS.UPDATED_BY, period.getUpdatedBy())
          .execute();

      period.setId(id);
      period.setCreatedAt(createdAt.toInstant(UTC));
      period.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      LocalDateTime updatedAt =
          period.getUpdatedAt() != null ? LocalDateTime.ofInstant(period.getUpdatedAt(), UTC) : now;

      dsl.update(PROPERTY_OCCUPANCY_PERIODS)
          .set(PROPERTY_OCCUPANCY_PERIODS.START_DATE, period.getStartDate())
          .set(PROPERTY_OCCUPANCY_PERIODS.END_DATE, period.getEndDate().orElse(null))
          .set(PROPERTY_OCCUPANCY_PERIODS.TYPE, period.getType().name())
          .set(PROPERTY_OCCUPANCY_PERIODS.OCCUPANT_NAME, period.getOccupantName().orElse(null))
          .set(
              PROPERTY_OCCUPANCY_PERIODS.MONTHLY_IMPUTED_RENT,
              period.getMonthlyImputedRent().orElse(null))
          .set(
              PROPERTY_OCCUPANCY_PERIODS.END_REASON,
              period.getEndReason().map(Enum::name).orElse(null))
          .set(PROPERTY_OCCUPANCY_PERIODS.NOTES, period.getNotes().orElse(null))
          .set(PROPERTY_OCCUPANCY_PERIODS.UPDATED_AT, updatedAt)
          .set(PROPERTY_OCCUPANCY_PERIODS.UPDATED_BY, period.getUpdatedBy())
          .where(
              PROPERTY_OCCUPANCY_PERIODS
                  .ID
                  .eq(period.getId())
                  .and(PROPERTY_OCCUPANCY_PERIODS.TEAM_ID.eq(period.getTeamId())))
          .execute();

      period.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return period;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTY_OCCUPANCY_PERIODS)
        .set(PROPERTY_OCCUPANCY_PERIODS.DELETED_AT, now)
        .where(
            PROPERTY_OCCUPANCY_PERIODS.ID.eq(id).and(PROPERTY_OCCUPANCY_PERIODS.TEAM_ID.eq(teamId)))
        .execute();
  }

  public int countByStatusAndTeamId(UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    return dsl.fetchCount(
        dsl.selectFrom(PROPERTY_OCCUPANCY_PERIODS)
            .where(
                PROPERTY_OCCUPANCY_PERIODS
                    .TEAM_ID
                    .eq(teamId)
                    .and(PROPERTY_OCCUPANCY_PERIODS.DELETED_AT.isNull())
                    .and(PROPERTY_OCCUPANCY_PERIODS.START_DATE.le(today))
                    .and(
                        PROPERTY_OCCUPANCY_PERIODS
                            .END_DATE
                            .isNull()
                            .or(PROPERTY_OCCUPANCY_PERIODS.END_DATE.ge(today)))));
  }
}
