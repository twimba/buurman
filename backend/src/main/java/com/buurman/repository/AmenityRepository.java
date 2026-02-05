package com.buurman.repository;

import com.buurman.domain.Amenity;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.buurman.jooq.generated.Tables.AMENITIES;

@Repository
public class AmenityRepository {

    private final DSLContext dsl;

    public AmenityRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Amenity> findAll() {
        return dsl.selectFrom(AMENITIES)
                .orderBy(AMENITIES.CATEGORY, AMENITIES.NAME)
                .fetch()
                .map(this::toDomain);
    }

    public Optional<Amenity> findByIdentifier(String identifier) {
        return dsl.selectFrom(AMENITIES)
                .where(AMENITIES.IDENTIFIER.eq(identifier))
                .fetchOptional()
                .map(this::toDomain);
    }

    public List<Amenity> findByCategory(String category) {
        return dsl.selectFrom(AMENITIES)
                .where(AMENITIES.CATEGORY.eq(category))
                .orderBy(AMENITIES.NAME)
                .fetch()
                .map(this::toDomain);
    }

    private Amenity toDomain(com.buurman.jooq.generated.tables.records.AmenitiesRecord record) {
        Amenity amenity = new Amenity();
        amenity.setId(record.getId());
        amenity.setIdentifier(record.getIdentifier());
        amenity.setName(record.getName());
        amenity.setCategory(record.getCategory());
        amenity.setIcon(record.getIcon());
        return amenity;
    }
}
