package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_AMENITIES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyAmenity;
import com.buurman.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyAmenityRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<PropertyAmenity> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(PROPERTY_AMENITIES)
            .where(
                PROPERTY_AMENITIES
                    .PROPERTY_ID
                    .eq(propertyId)
                    .and(PROPERTY_AMENITIES.TEAM_ID.eq(teamId))
                    .and(PROPERTY_AMENITIES.DELETED_AT.isNull()))
            .orderBy(PROPERTY_AMENITIES.CREATED_AT.asc())
            .fetch()
            .map(this::toDomain));
  }

  public Optional<PropertyAmenity> findByPropertyIdAndAmenityIdAndTeamId(
      UUID propertyId, UUID amenityId, UUID teamId) {
    return dsl.selectFrom(PROPERTY_AMENITIES)
        .where(
            PROPERTY_AMENITIES
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_AMENITIES.AMENITY_ID.eq(amenityId))
                .and(PROPERTY_AMENITIES.TEAM_ID.eq(teamId))
                .and(PROPERTY_AMENITIES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PropertyAmenity getByPropertyIdAndAmenityIdAndTeamId(
      UUID propertyId, UUID amenityId, UUID teamId) {
    return findByPropertyIdAndAmenityIdAndTeamId(propertyId, amenityId, teamId)
        .orElseThrow(() -> new NotFoundException("Property amenity not found"));
  }

  public PropertyAmenity save(PropertyAmenity pa) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (pa.getId() == null) {
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt =
          pa.getCreatedAt() != null ? LocalDateTime.ofInstant(pa.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          pa.getUpdatedAt() != null ? LocalDateTime.ofInstant(pa.getUpdatedAt(), UTC) : now;

      dsl.insertInto(PROPERTY_AMENITIES)
          .set(PROPERTY_AMENITIES.ID, newId)
          .set(PROPERTY_AMENITIES.PROPERTY_ID, pa.getPropertyId())
          .set(PROPERTY_AMENITIES.AMENITY_ID, pa.getAmenityId())
          .set(PROPERTY_AMENITIES.TEAM_ID, pa.getTeamId())
          .set(PROPERTY_AMENITIES.NOTES, pa.getNotes())
          .set(PROPERTY_AMENITIES.CREATED_AT, createdAt)
          .set(PROPERTY_AMENITIES.UPDATED_AT, updatedAt)
          .set(PROPERTY_AMENITIES.CREATED_BY, pa.getCreatedBy())
          .set(PROPERTY_AMENITIES.UPDATED_BY, pa.getUpdatedBy())
          .execute();

      pa.setId(newId);
      pa.setCreatedAt(createdAt.toInstant(UTC));
      pa.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      LocalDateTime updatedAt =
          pa.getUpdatedAt() != null ? LocalDateTime.ofInstant(pa.getUpdatedAt(), UTC) : now;

      dsl.update(PROPERTY_AMENITIES)
          .set(PROPERTY_AMENITIES.NOTES, pa.getNotes())
          .set(PROPERTY_AMENITIES.UPDATED_AT, updatedAt)
          .set(PROPERTY_AMENITIES.UPDATED_BY, pa.getUpdatedBy())
          .where(
              PROPERTY_AMENITIES
                  .ID
                  .eq(pa.getId())
                  .and(PROPERTY_AMENITIES.TEAM_ID.eq(pa.getTeamId())))
          .execute();

      pa.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return pa;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTY_AMENITIES)
        .set(PROPERTY_AMENITIES.DELETED_AT, now)
        .where(PROPERTY_AMENITIES.ID.eq(id).and(PROPERTY_AMENITIES.TEAM_ID.eq(teamId)))
        .execute();
  }

  private PropertyAmenity toDomain(
      com.buurman.jooq.generated.tables.records.PropertyAmenitiesRecord record) {
    PropertyAmenity pa = new PropertyAmenity();
    pa.setId(record.getId());
    pa.setPropertyId(record.getPropertyId());
    pa.setAmenityId(record.getAmenityId());
    pa.setTeamId(record.getTeamId());
    pa.setNotes(record.getNotes());
    pa.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    pa.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    pa.setCreatedBy(record.getCreatedBy());
    pa.setUpdatedBy(record.getUpdatedBy());
    pa.setDeletedAt(record.getDeletedAt() == null ? null : record.getDeletedAt().toInstant(UTC));
    return pa;
  }
}
