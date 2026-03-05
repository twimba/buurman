package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_TAXES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyTax;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PropertyTaxRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyTaxRepository {

  private final DSLContext dsl;
  private final PropertyTaxRecordMapper mapper;
  private final Clock clock;

  public Optional<PropertyTax> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(PROPERTY_TAXES)
        .where(
            PROPERTY_TAXES
                .IDENTIFIER
                .eq(identifier)
                .and(PROPERTY_TAXES.TEAM_ID.eq(teamId))
                .and(PROPERTY_TAXES.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public PropertyTax getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Property tax not found"));
  }

  public List<PropertyTax> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_TAXES)
        .where(
            PROPERTY_TAXES
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_TAXES.TEAM_ID.eq(teamId))
                .and(PROPERTY_TAXES.DELETED_AT.isNull()))
        .orderBy(PROPERTY_TAXES.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<PropertyTax> findActiveByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_TAXES)
        .where(
            PROPERTY_TAXES
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_TAXES.TEAM_ID.eq(teamId))
                .and(PROPERTY_TAXES.STATUS.eq(PropertyTax.TaxStatus.ACTIVE.name()))
                .and(PROPERTY_TAXES.DELETED_AT.isNull()))
        .orderBy(PROPERTY_TAXES.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public PropertyTax save(PropertyTax tax) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (tax.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          tax.getCreatedAt() != null ? LocalDateTime.ofInstant(tax.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          tax.getUpdatedAt() != null ? LocalDateTime.ofInstant(tax.getUpdatedAt(), UTC) : now;

      dsl.insertInto(PROPERTY_TAXES)
          .set(PROPERTY_TAXES.ID, id)
          .set(PROPERTY_TAXES.IDENTIFIER, tax.getIdentifier().orElseThrow())
          .set(PROPERTY_TAXES.PROPERTY_ID, tax.getPropertyId())
          .set(PROPERTY_TAXES.TEAM_ID, tax.getTeamId())
          .set(PROPERTY_TAXES.TAX_TYPE, tax.getTaxType().name())
          .set(PROPERTY_TAXES.AUTHORITY, tax.getAuthority().orElse(null))
          .set(PROPERTY_TAXES.ANNUAL_AMOUNT, tax.getAnnualAmount().value())
          .set(PROPERTY_TAXES.CURRENCY, tax.getAnnualAmount().currency())
          .set(PROPERTY_TAXES.PAYMENT_FREQUENCY, tax.getPaymentFrequency())
          .set(PROPERTY_TAXES.DUE_MONTHS, tax.getDueMonths().orElse(null))
          .set(PROPERTY_TAXES.TAX_YEAR, tax.getTaxYear().orElse(null))
          .set(PROPERTY_TAXES.START_DATE, tax.getStartDate().orElse(null))
          .set(PROPERTY_TAXES.END_DATE, tax.getEndDate().orElse(null))
          .set(PROPERTY_TAXES.STATUS, tax.getStatus().name())
          .set(PROPERTY_TAXES.NOTES, tax.getNotes().orElse(null))
          .set(PROPERTY_TAXES.CREATED_AT, createdAt)
          .set(PROPERTY_TAXES.UPDATED_AT, updatedAt)
          .set(PROPERTY_TAXES.CREATED_BY, tax.getCreatedBy())
          .set(PROPERTY_TAXES.UPDATED_BY, tax.getUpdatedBy())
          .execute();

      tax.setId(id);
      tax.setCreatedAt(createdAt.toInstant(UTC));
      tax.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // Update
      LocalDateTime updatedAt =
          tax.getUpdatedAt() != null ? LocalDateTime.ofInstant(tax.getUpdatedAt(), UTC) : now;

      dsl.update(PROPERTY_TAXES)
          .set(PROPERTY_TAXES.TAX_TYPE, tax.getTaxType().name())
          .set(PROPERTY_TAXES.AUTHORITY, tax.getAuthority().orElse(null))
          .set(PROPERTY_TAXES.ANNUAL_AMOUNT, tax.getAnnualAmount().value())
          .set(PROPERTY_TAXES.CURRENCY, tax.getAnnualAmount().currency())
          .set(PROPERTY_TAXES.PAYMENT_FREQUENCY, tax.getPaymentFrequency())
          .set(PROPERTY_TAXES.DUE_MONTHS, tax.getDueMonths().orElse(null))
          .set(PROPERTY_TAXES.TAX_YEAR, tax.getTaxYear().orElse(null))
          .set(PROPERTY_TAXES.START_DATE, tax.getStartDate().orElse(null))
          .set(PROPERTY_TAXES.END_DATE, tax.getEndDate().orElse(null))
          .set(PROPERTY_TAXES.STATUS, tax.getStatus().name())
          .set(PROPERTY_TAXES.NOTES, tax.getNotes().orElse(null))
          .set(PROPERTY_TAXES.UPDATED_AT, updatedAt)
          .set(PROPERTY_TAXES.UPDATED_BY, tax.getUpdatedBy())
          .where(PROPERTY_TAXES.ID.eq(tax.getId()).and(PROPERTY_TAXES.TEAM_ID.eq(tax.getTeamId())))
          .execute();

      tax.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return tax;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTY_TAXES)
        .set(PROPERTY_TAXES.DELETED_AT, now)
        .where(PROPERTY_TAXES.ID.eq(id).and(PROPERTY_TAXES.TEAM_ID.eq(teamId)))
        .execute();
  }
}
