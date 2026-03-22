package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_RESIDENTIAL_DETAILS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyResidentialDetails;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyResidentialDetailsRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public Optional<PropertyResidentialDetails> findByPropertyIdAndTeamId(
      UUID propertyId, UUID teamId) {
    return dsl.selectFrom(PROPERTY_RESIDENTIAL_DETAILS)
        .where(
            PROPERTY_RESIDENTIAL_DETAILS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_RESIDENTIAL_DETAILS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(this::toDomain);
  }

  public void save(PropertyResidentialDetails details) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (details.getId() == null) {
      UUID newId = UUID.randomUUID();
      dsl.insertInto(PROPERTY_RESIDENTIAL_DETAILS)
          .set(PROPERTY_RESIDENTIAL_DETAILS.ID, newId)
          .set(PROPERTY_RESIDENTIAL_DETAILS.PROPERTY_ID, details.getPropertyId())
          .set(PROPERTY_RESIDENTIAL_DETAILS.TEAM_ID, details.getTeamId())
          .set(PROPERTY_RESIDENTIAL_DETAILS.BEDROOMS, details.getBedrooms().orElse(null))
          .set(PROPERTY_RESIDENTIAL_DETAILS.BATHROOMS, details.getBathrooms().orElse(null))
          .set(PROPERTY_RESIDENTIAL_DETAILS.FURNISHED, details.getFurnished().orElse(null))
          .set(PROPERTY_RESIDENTIAL_DETAILS.PET_POLICY, details.getPetPolicy().orElse(null))
          .set(PROPERTY_RESIDENTIAL_DETAILS.CREATED_AT, now)
          .set(PROPERTY_RESIDENTIAL_DETAILS.UPDATED_AT, now)
          .set(PROPERTY_RESIDENTIAL_DETAILS.CREATED_BY, details.getCreatedBy())
          .set(PROPERTY_RESIDENTIAL_DETAILS.UPDATED_BY, details.getUpdatedBy())
          .execute();
      details.setId(newId);
      details.setCreatedAt(now.toInstant(UTC));
      details.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(PROPERTY_RESIDENTIAL_DETAILS)
          .set(PROPERTY_RESIDENTIAL_DETAILS.BEDROOMS, details.getBedrooms().orElse(null))
          .set(PROPERTY_RESIDENTIAL_DETAILS.BATHROOMS, details.getBathrooms().orElse(null))
          .set(PROPERTY_RESIDENTIAL_DETAILS.FURNISHED, details.getFurnished().orElse(null))
          .set(PROPERTY_RESIDENTIAL_DETAILS.PET_POLICY, details.getPetPolicy().orElse(null))
          .set(PROPERTY_RESIDENTIAL_DETAILS.UPDATED_AT, now)
          .set(PROPERTY_RESIDENTIAL_DETAILS.UPDATED_BY, details.getUpdatedBy())
          .where(
              PROPERTY_RESIDENTIAL_DETAILS
                  .ID
                  .eq(details.getId())
                  .and(PROPERTY_RESIDENTIAL_DETAILS.TEAM_ID.eq(details.getTeamId())))
          .execute();
      details.setUpdatedAt(now.toInstant(UTC));
    }
  }

  public void deleteByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    dsl.deleteFrom(PROPERTY_RESIDENTIAL_DETAILS)
        .where(
            PROPERTY_RESIDENTIAL_DETAILS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_RESIDENTIAL_DETAILS.TEAM_ID.eq(teamId)))
        .execute();
  }

  private PropertyResidentialDetails toDomain(
      com.buurman.jooq.generated.tables.records.PropertyResidentialDetailsRecord record) {
    PropertyResidentialDetails d = new PropertyResidentialDetails();
    d.setId(record.getId());
    d.setPropertyId(record.getPropertyId());
    d.setTeamId(record.getTeamId());
    d.setBedrooms(Optional.ofNullable(record.getBedrooms()));
    d.setBathrooms(Optional.ofNullable(record.getBathrooms()));
    d.setFurnished(Optional.ofNullable(record.getFurnished()));
    d.setPetPolicy(Optional.ofNullable(record.getPetPolicy()));
    d.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    d.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    d.setCreatedBy(record.getCreatedBy());
    d.setUpdatedBy(record.getUpdatedBy());
    return d;
  }
}
