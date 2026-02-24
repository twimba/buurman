package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.AMENITIES;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Amenity;
import com.buurman.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AmenityRepository {

  private final DSLContext dsl;

  public List<Amenity> findAll() {
    return List.copyOf(
        dsl.selectFrom(AMENITIES)
            .orderBy(AMENITIES.CATEGORY, AMENITIES.NAME)
            .fetch()
            .map(this::toDomain));
  }

  public Optional<Amenity> findByIdentifier(String identifier) {
    return dsl.selectFrom(AMENITIES)
        .where(AMENITIES.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .map(this::toDomain);
  }

  public Amenity getByIdentifier(String identifier) {
    return findByIdentifier(identifier)
        .orElseThrow(() -> new NotFoundException("Amenity not found"));
  }

  public List<Amenity> findByApplicableCategory(String propertyCategory) {
    Condition condition =
        DSL.condition("{0} = ANY({1})", DSL.val(propertyCategory), AMENITIES.APPLICABLE_CATEGORIES);
    return List.copyOf(
        dsl.selectFrom(AMENITIES)
            .where(condition)
            .orderBy(AMENITIES.CATEGORY, AMENITIES.NAME)
            .fetch()
            .map(this::toDomain));
  }

  private Amenity toDomain(com.buurman.jooq.generated.tables.records.AmenitiesRecord record) {
    Amenity amenity = new Amenity();
    amenity.setId(record.getId());
    amenity.setIdentifier(record.getIdentifier());
    amenity.setName(record.getName());
    amenity.setCategory(record.getCategory());
    amenity.setIcon(record.getIcon());
    if (record.getApplicableCategories() != null) {
      amenity.setApplicableCategories(Arrays.asList(record.getApplicableCategories()));
    }
    return amenity;
  }
}
