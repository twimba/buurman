package com.buurman.repository;

import com.buurman.domain.Property;
import com.buurman.mapper.PropertyRecordMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.PROPERTIES;

@Repository
public class PropertyRepository {

    private final DSLContext dsl;
    private final PropertyRecordMapper mapper;

    public PropertyRepository(DSLContext dsl, PropertyRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    public Optional<Property> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(PROPERTIES)
                .where(PROPERTIES.ID.eq(id)
                        .and(PROPERTIES.TEAM_ID.eq(teamId))
                        .and(PROPERTIES.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<Property> findAllByTeamId(UUID teamId) {
        return dsl.selectFrom(PROPERTIES)
                .where(PROPERTIES.TEAM_ID.eq(teamId)
                        .and(PROPERTIES.DELETED_AT.isNull()))
                .orderBy(PROPERTIES.CREATED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Property> findByTeamIdAndStatus(UUID teamId, Property.PropertyStatus status) {
        return dsl.selectFrom(PROPERTIES)
                .where(PROPERTIES.TEAM_ID.eq(teamId)
                        .and(PROPERTIES.STATUS.eq(status.name()))
                        .and(PROPERTIES.DELETED_AT.isNull()))
                .orderBy(PROPERTIES.CREATED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public Property save(Property property) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (property.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            dsl.insertInto(PROPERTIES)
                    .set(PROPERTIES.ID, newId)
                    .set(PROPERTIES.IDENTIFIER, property.getIdentifier())
                    .set(PROPERTIES.TEAM_ID, property.getTeamId())
                    .set(PROPERTIES.STREET, property.getStreet())
                    .set(PROPERTIES.CITY, property.getCity())
                    .set(PROPERTIES.POSTAL_CODE, property.getPostalCode())
                    .set(PROPERTIES.COUNTRY, property.getCountry())
                    .set(PROPERTIES.BEDROOMS, property.getBedrooms())
                    .set(PROPERTIES.BATHROOMS, property.getBathrooms())
                    .set(PROPERTIES.SQUARE_METERS, property.getSquareMeters())
                    .set(PROPERTIES.PROPERTY_TYPE, property.getPropertyType().name())
                    .set(PROPERTIES.STATUS, property.getStatus().name())
                    .set(PROPERTIES.CREATED_AT, now)
                    .set(PROPERTIES.UPDATED_AT, now)
                    .set(PROPERTIES.CREATED_BY, property.getCreatedBy())
                    .set(PROPERTIES.UPDATED_BY, property.getUpdatedBy())
                    .execute();

            property.setId(newId);
            property.setCreatedAt(now.toInstant(ZoneOffset.UTC));
            property.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            // UPDATE
            dsl.update(PROPERTIES)
                    .set(PROPERTIES.STREET, property.getStreet())
                    .set(PROPERTIES.CITY, property.getCity())
                    .set(PROPERTIES.POSTAL_CODE, property.getPostalCode())
                    .set(PROPERTIES.COUNTRY, property.getCountry())
                    .set(PROPERTIES.BEDROOMS, property.getBedrooms())
                    .set(PROPERTIES.BATHROOMS, property.getBathrooms())
                    .set(PROPERTIES.SQUARE_METERS, property.getSquareMeters())
                    .set(PROPERTIES.PROPERTY_TYPE, property.getPropertyType().name())
                    .set(PROPERTIES.STATUS, property.getStatus().name())
                    .set(PROPERTIES.UPDATED_AT, now)
                    .set(PROPERTIES.UPDATED_BY, property.getUpdatedBy())
                    .where(PROPERTIES.ID.eq(property.getId())
                            .and(PROPERTIES.TEAM_ID.eq(property.getTeamId())))
                    .execute();

            property.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }

        return property;
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(PROPERTIES)
                .set(PROPERTIES.DELETED_AT, now)
                .where(PROPERTIES.ID.eq(id)
                        .and(PROPERTIES.TEAM_ID.eq(teamId)))
                .execute();
    }
}
