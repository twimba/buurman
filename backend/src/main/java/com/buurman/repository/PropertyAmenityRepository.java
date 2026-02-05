package com.buurman.repository;

import com.buurman.domain.PropertyAmenity;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.PROPERTY_AMENITIES;

@Repository
public class PropertyAmenityRepository {

    private final DSLContext dsl;

    public PropertyAmenityRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<PropertyAmenity> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
        return dsl.selectFrom(PROPERTY_AMENITIES)
                .where(PROPERTY_AMENITIES.PROPERTY_ID.eq(propertyId)
                        .and(PROPERTY_AMENITIES.TEAM_ID.eq(teamId))
                        .and(PROPERTY_AMENITIES.DELETED_AT.isNull()))
                .orderBy(PROPERTY_AMENITIES.CREATED_AT.asc())
                .fetch()
                .map(this::toDomain);
    }

    public Optional<PropertyAmenity> findByPropertyIdAndAmenityIdAndTeamId(UUID propertyId, UUID amenityId, UUID teamId) {
        return dsl.selectFrom(PROPERTY_AMENITIES)
                .where(PROPERTY_AMENITIES.PROPERTY_ID.eq(propertyId)
                        .and(PROPERTY_AMENITIES.AMENITY_ID.eq(amenityId))
                        .and(PROPERTY_AMENITIES.TEAM_ID.eq(teamId))
                        .and(PROPERTY_AMENITIES.DELETED_AT.isNull()))
                .fetchOptional()
                .map(this::toDomain);
    }

    public PropertyAmenity save(PropertyAmenity pa) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (pa.getId() == null) {
            UUID newId = UUID.randomUUID();
            dsl.insertInto(PROPERTY_AMENITIES)
                    .set(PROPERTY_AMENITIES.ID, newId)
                    .set(PROPERTY_AMENITIES.PROPERTY_ID, pa.getPropertyId())
                    .set(PROPERTY_AMENITIES.AMENITY_ID, pa.getAmenityId())
                    .set(PROPERTY_AMENITIES.TEAM_ID, pa.getTeamId())
                    .set(PROPERTY_AMENITIES.NOTES, pa.getNotes())
                    .set(PROPERTY_AMENITIES.CREATED_AT, now)
                    .set(PROPERTY_AMENITIES.UPDATED_AT, now)
                    .set(PROPERTY_AMENITIES.CREATED_BY, pa.getCreatedBy())
                    .set(PROPERTY_AMENITIES.UPDATED_BY, pa.getUpdatedBy())
                    .execute();

            pa.setId(newId);
            pa.setCreatedAt(now.toInstant(ZoneOffset.UTC));
            pa.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            dsl.update(PROPERTY_AMENITIES)
                    .set(PROPERTY_AMENITIES.NOTES, pa.getNotes())
                    .set(PROPERTY_AMENITIES.UPDATED_AT, now)
                    .set(PROPERTY_AMENITIES.UPDATED_BY, pa.getUpdatedBy())
                    .where(PROPERTY_AMENITIES.ID.eq(pa.getId())
                            .and(PROPERTY_AMENITIES.TEAM_ID.eq(pa.getTeamId())))
                    .execute();

            pa.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }

        return pa;
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(PROPERTY_AMENITIES)
                .set(PROPERTY_AMENITIES.DELETED_AT, now)
                .where(PROPERTY_AMENITIES.ID.eq(id)
                        .and(PROPERTY_AMENITIES.TEAM_ID.eq(teamId)))
                .execute();
    }

    private PropertyAmenity toDomain(com.buurman.jooq.generated.tables.records.PropertyAmenitiesRecord record) {
        PropertyAmenity pa = new PropertyAmenity();
        pa.setId(record.getId());
        pa.setPropertyId(record.getPropertyId());
        pa.setAmenityId(record.getAmenityId());
        pa.setTeamId(record.getTeamId());
        pa.setNotes(record.getNotes());
        pa.setCreatedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().toInstant(ZoneOffset.UTC));
        pa.setUpdatedAt(record.getUpdatedAt() == null ? null : record.getUpdatedAt().toInstant(ZoneOffset.UTC));
        pa.setCreatedBy(record.getCreatedBy());
        pa.setUpdatedBy(record.getUpdatedBy());
        pa.setDeletedAt(record.getDeletedAt() == null ? null : record.getDeletedAt().toInstant(ZoneOffset.UTC));
        return pa;
    }
}
