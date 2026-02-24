package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_OUTDOOR_AREAS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyOutdoorArea;
import com.buurman.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyOutdoorAreaRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<PropertyOutdoorArea> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(PROPERTY_OUTDOOR_AREAS)
            .where(
                PROPERTY_OUTDOOR_AREAS
                    .PROPERTY_ID
                    .eq(propertyId)
                    .and(PROPERTY_OUTDOOR_AREAS.TEAM_ID.eq(teamId))
                    .and(PROPERTY_OUTDOOR_AREAS.DELETED_AT.isNull()))
            .orderBy(PROPERTY_OUTDOOR_AREAS.CREATED_AT.asc())
            .fetch()
            .map(this::toDomain));
  }

  public Optional<PropertyOutdoorArea> findByIdentifierAndTeamId(String identifier, UUID teamId) {
    return dsl.selectFrom(PROPERTY_OUTDOOR_AREAS)
        .where(
            PROPERTY_OUTDOOR_AREAS
                .IDENTIFIER
                .eq(identifier)
                .and(PROPERTY_OUTDOOR_AREAS.TEAM_ID.eq(teamId))
                .and(PROPERTY_OUTDOOR_AREAS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PropertyOutdoorArea getByIdentifierAndTeamId(String identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Outdoor area not found"));
  }

  public PropertyOutdoorArea save(PropertyOutdoorArea area) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (area.getId() == null) {
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt = LocalDateTime.ofInstant(area.getCreatedAt(), UTC);
      LocalDateTime updatedAt = LocalDateTime.ofInstant(area.getUpdatedAt(), UTC);

      dsl.insertInto(PROPERTY_OUTDOOR_AREAS)
          .set(PROPERTY_OUTDOOR_AREAS.ID, newId)
          .set(PROPERTY_OUTDOOR_AREAS.IDENTIFIER, area.getIdentifier())
          .set(PROPERTY_OUTDOOR_AREAS.PROPERTY_ID, area.getPropertyId())
          .set(PROPERTY_OUTDOOR_AREAS.TEAM_ID, area.getTeamId())
          .set(PROPERTY_OUTDOOR_AREAS.TYPE, area.getType())
          .set(PROPERTY_OUTDOOR_AREAS.AREA_VALUE, area.getAreaValue().orElse(null))
          .set(PROPERTY_OUTDOOR_AREAS.AREA_UNIT, area.getAreaUnit())
          .set(PROPERTY_OUTDOOR_AREAS.CREATED_AT, createdAt)
          .set(PROPERTY_OUTDOOR_AREAS.UPDATED_AT, updatedAt)
          .set(PROPERTY_OUTDOOR_AREAS.CREATED_BY, area.getCreatedBy())
          .set(PROPERTY_OUTDOOR_AREAS.UPDATED_BY, area.getUpdatedBy())
          .execute();

      area.setId(newId);
      area.setCreatedAt(createdAt.toInstant(UTC));
      area.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      LocalDateTime updatedAt = LocalDateTime.ofInstant(area.getUpdatedAt(), UTC);

      dsl.update(PROPERTY_OUTDOOR_AREAS)
          .set(PROPERTY_OUTDOOR_AREAS.TYPE, area.getType())
          .set(PROPERTY_OUTDOOR_AREAS.AREA_VALUE, area.getAreaValue().orElse(null))
          .set(PROPERTY_OUTDOOR_AREAS.AREA_UNIT, area.getAreaUnit())
          .set(PROPERTY_OUTDOOR_AREAS.UPDATED_AT, updatedAt)
          .set(PROPERTY_OUTDOOR_AREAS.UPDATED_BY, area.getUpdatedBy())
          .where(
              PROPERTY_OUTDOOR_AREAS
                  .ID
                  .eq(area.getId())
                  .and(PROPERTY_OUTDOOR_AREAS.TEAM_ID.eq(area.getTeamId())))
          .execute();

      area.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return area;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTY_OUTDOOR_AREAS)
        .set(PROPERTY_OUTDOOR_AREAS.DELETED_AT, now)
        .where(PROPERTY_OUTDOOR_AREAS.ID.eq(id).and(PROPERTY_OUTDOOR_AREAS.TEAM_ID.eq(teamId)))
        .execute();
  }

  private PropertyOutdoorArea toDomain(
      com.buurman.jooq.generated.tables.records.PropertyOutdoorAreasRecord record) {
    PropertyOutdoorArea area = new PropertyOutdoorArea();
    area.setId(record.getId());
    area.setIdentifier(record.getIdentifier());
    area.setPropertyId(record.getPropertyId());
    area.setTeamId(record.getTeamId());
    area.setType(record.getType());
    area.setAreaValue(Optional.ofNullable(record.getAreaValue()));
    area.setAreaUnit(record.getAreaUnit());
    area.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    area.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    area.setCreatedBy(record.getCreatedBy());
    area.setUpdatedBy(record.getUpdatedBy());
    area.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));
    return area;
  }
}
