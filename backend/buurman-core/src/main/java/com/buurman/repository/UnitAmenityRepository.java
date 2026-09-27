package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.AMENITIES;
import static com.buurman.jooq.generated.Tables.UNITS;
import static com.buurman.jooq.generated.Tables.UNIT_AMENITIES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Amenity;
import com.buurman.domain.UnitAmenity;
import com.buurman.jooq.generated.tables.records.AmenitiesRecord;
import com.buurman.jooq.generated.tables.records.UnitAmenitiesRecord;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UnitAmenityRepository {

  private final DSLContext dsl;
  private final Clock clock;

  /** Active amenities linked to the unit, joined with the shared catalogue. */
  public List<Amenity> findAmenitiesByUnitIdAndTeamId(UUID unitId, UUID teamId) {
    return List.copyOf(
        dsl.select(AMENITIES.fields())
            .from(UNIT_AMENITIES)
            .join(AMENITIES)
            .on(AMENITIES.ID.eq(UNIT_AMENITIES.AMENITY_ID))
            .where(
                UNIT_AMENITIES
                    .UNIT_ID
                    .eq(unitId)
                    .and(UNIT_AMENITIES.TEAM_ID.eq(teamId))
                    .and(UNIT_AMENITIES.DELETED_AT.isNull()))
            .orderBy(AMENITIES.CATEGORY.asc(), AMENITIES.NAME.asc())
            .fetchInto(AMENITIES)
            .map(this::amenityToDomain));
  }

  /**
   * Every link row for the unit, active or soft-deleted. Used to diff a desired amenity set against
   * what already exists — the {@code uq_unit_amenities} constraint is not partial, so a
   * previously-removed link must be reactivated in place rather than re-inserted.
   */
  public List<UnitAmenity> findAllLinksByUnitIdAndTeamId(UUID unitId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(UNIT_AMENITIES)
            .where(UNIT_AMENITIES.UNIT_ID.eq(unitId).and(UNIT_AMENITIES.TEAM_ID.eq(teamId)))
            .fetch()
            .map(this::toDomain));
  }

  public UnitAmenity insertLink(UUID unitId, UUID amenityId, UUID teamId, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID newId = UUID.randomUUID();

    dsl.insertInto(UNIT_AMENITIES)
        .set(UNIT_AMENITIES.ID, newId)
        .set(UNIT_AMENITIES.UNIT_ID, unitId)
        .set(UNIT_AMENITIES.AMENITY_ID, amenityId)
        .set(UNIT_AMENITIES.TEAM_ID, teamId)
        .set(UNIT_AMENITIES.CREATED_AT, now)
        .set(UNIT_AMENITIES.UPDATED_AT, now)
        .set(UNIT_AMENITIES.CREATED_BY, actorId)
        .set(UNIT_AMENITIES.UPDATED_BY, actorId)
        .execute();

    UnitAmenity link = new UnitAmenity();
    link.setId(newId);
    link.setUnitId(unitId);
    link.setAmenityId(amenityId);
    link.setTeamId(teamId);
    link.setCreatedAt(Optional.of(now.toInstant(UTC)));
    link.setUpdatedAt(Optional.of(now.toInstant(UTC)));
    link.setCreatedBy(Optional.of(actorId));
    link.setUpdatedBy(Optional.of(actorId));
    return link;
  }

  public void reactivateLink(UUID linkId, UUID teamId, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(UNIT_AMENITIES)
        .set(UNIT_AMENITIES.DELETED_AT, (LocalDateTime) null)
        .set(UNIT_AMENITIES.UPDATED_AT, now)
        .set(UNIT_AMENITIES.UPDATED_BY, actorId)
        .where(UNIT_AMENITIES.ID.eq(linkId).and(UNIT_AMENITIES.TEAM_ID.eq(teamId)))
        .execute();
  }

  public void softDeleteLink(UUID linkId, UUID teamId, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(UNIT_AMENITIES)
        .set(UNIT_AMENITIES.DELETED_AT, now)
        .set(UNIT_AMENITIES.UPDATED_AT, now)
        .set(UNIT_AMENITIES.UPDATED_BY, actorId)
        .where(UNIT_AMENITIES.ID.eq(linkId).and(UNIT_AMENITIES.TEAM_ID.eq(teamId)))
        .execute();
  }

  /**
   * Soft-deletes every amenity link of every unit of {@code propertyId}, in one statement.
   * PropertyService#deleteProperty calls this in the same transaction it soft-deletes the
   * property's units, so a deleted property never leaves an active amenity link behind on a unit
   * that {@code UnitRepository}'s single-row lookups (which deliberately skip the properties join)
   * would otherwise still resolve.
   */
  public int softDeleteAllByPropertyIdAndTeamId(UUID propertyId, UUID teamId, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    return dsl.update(UNIT_AMENITIES)
        .set(UNIT_AMENITIES.DELETED_AT, now)
        .set(UNIT_AMENITIES.UPDATED_AT, now)
        .set(UNIT_AMENITIES.UPDATED_BY, actorId)
        .where(
            UNIT_AMENITIES
                .TEAM_ID
                .eq(teamId)
                .and(UNIT_AMENITIES.DELETED_AT.isNull())
                .and(
                    UNIT_AMENITIES.UNIT_ID.in(
                        DSL.select(UNITS.ID)
                            .from(UNITS)
                            .where(
                                UNITS.PROPERTY_ID.eq(propertyId).and(UNITS.TEAM_ID.eq(teamId))))))
        .execute();
  }

  private Amenity amenityToDomain(AmenitiesRecord record) {
    Amenity amenity = new Amenity();
    amenity.setId(record.getId());
    amenity.setIdentifier(Optional.of(record.getIdentifier()));
    amenity.setName(record.getName());
    amenity.setCategory(record.getCategory());
    amenity.setIcon(record.getIcon());
    if (record.getApplicableCategories() != null) {
      amenity.setApplicableCategories(Arrays.asList(record.getApplicableCategories()));
    }
    return amenity;
  }

  private UnitAmenity toDomain(UnitAmenitiesRecord record) {
    UnitAmenity link = new UnitAmenity();
    link.setId(record.getId());
    link.setUnitId(record.getUnitId());
    link.setAmenityId(record.getAmenityId());
    link.setTeamId(record.getTeamId());
    link.setNotes(Optional.ofNullable(record.getNotes()));
    link.setCreatedAt(Optional.ofNullable(record.getCreatedAt()).map(dt -> dt.toInstant(UTC)));
    link.setUpdatedAt(Optional.ofNullable(record.getUpdatedAt()).map(dt -> dt.toInstant(UTC)));
    link.setCreatedBy(Optional.ofNullable(record.getCreatedBy()));
    link.setUpdatedBy(Optional.ofNullable(record.getUpdatedBy()));
    link.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));
    return link;
  }
}
