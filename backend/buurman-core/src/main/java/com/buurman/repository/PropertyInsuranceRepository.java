package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_INSURANCES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyInsurance;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PropertyInsuranceRecordMapper;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyInsuranceRepository {

  private final DSLContext dsl;
  private final PropertyInsuranceRecordMapper mapper;
  private final Clock clock;

  public Optional<PropertyInsurance> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(PROPERTY_INSURANCES)
        .where(
            PROPERTY_INSURANCES
                .IDENTIFIER
                .eq(identifier)
                .and(PROPERTY_INSURANCES.TEAM_ID.eq(teamId))
                .and(PROPERTY_INSURANCES.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public PropertyInsurance getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Property insurance not found"));
  }

  public List<PropertyInsurance> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_INSURANCES)
        .where(
            PROPERTY_INSURANCES
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_INSURANCES.TEAM_ID.eq(teamId))
                .and(PROPERTY_INSURANCES.DELETED_AT.isNull()))
        .orderBy(PROPERTY_INSURANCES.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<PropertyInsurance> findActiveByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_INSURANCES)
        .where(
            PROPERTY_INSURANCES
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_INSURANCES.TEAM_ID.eq(teamId))
                .and(PROPERTY_INSURANCES.STATUS.eq(PropertyInsurance.InsuranceStatus.ACTIVE.name()))
                .and(PROPERTY_INSURANCES.DELETED_AT.isNull()))
        .orderBy(PROPERTY_INSURANCES.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public PropertyInsurance save(PropertyInsurance insurance) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (insurance.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          insurance.getCreatedAt() != null
              ? LocalDateTime.ofInstant(insurance.getCreatedAt(), UTC)
              : now;
      LocalDateTime updatedAt =
          insurance.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(insurance.getUpdatedAt(), UTC)
              : now;

      dsl.insertInto(PROPERTY_INSURANCES)
          .set(PROPERTY_INSURANCES.ID, id)
          .set(PROPERTY_INSURANCES.IDENTIFIER, insurance.getIdentifier().orElseThrow())
          .set(PROPERTY_INSURANCES.PROPERTY_ID, insurance.getPropertyId())
          .set(PROPERTY_INSURANCES.TEAM_ID, insurance.getTeamId())
          .set(PROPERTY_INSURANCES.INSURANCE_TYPE, insurance.getInsuranceType().name())
          .set(PROPERTY_INSURANCES.PROVIDER, insurance.getProvider().orElse(null))
          .set(PROPERTY_INSURANCES.POLICY_NUMBER, insurance.getPolicyNumber().orElse(null))
          .set(
              PROPERTY_INSURANCES.COVERAGE_AMOUNT,
              insurance.getCoverageAmount().map(MoneyAmount::value).orElse(null))
          .set(
              PROPERTY_INSURANCES.COVERAGE_AMOUNT_CURRENCY,
              insurance.getCoverageAmount().map(MoneyAmount::currency).orElse(null))
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM, insurance.getAnnualPremium().value())
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM_CURRENCY, insurance.getAnnualPremium().currency())
          .set(PROPERTY_INSURANCES.PAYMENT_FREQUENCY, insurance.getPaymentFrequency())
          .set(PROPERTY_INSURANCES.START_DATE, insurance.getStartDate().orElse(null))
          .set(PROPERTY_INSURANCES.END_DATE, insurance.getEndDate().orElse(null))
          .set(PROPERTY_INSURANCES.STATUS, insurance.getStatus().name())
          .set(PROPERTY_INSURANCES.NOTES, insurance.getNotes().orElse(null))
          .set(PROPERTY_INSURANCES.CREATED_AT, createdAt)
          .set(PROPERTY_INSURANCES.UPDATED_AT, updatedAt)
          .set(PROPERTY_INSURANCES.CREATED_BY, insurance.getCreatedBy())
          .set(PROPERTY_INSURANCES.UPDATED_BY, insurance.getUpdatedBy())
          .execute();

      insurance.setId(id);
      insurance.setCreatedAt(createdAt.toInstant(UTC));
      insurance.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // Update
      LocalDateTime updatedAt =
          insurance.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(insurance.getUpdatedAt(), UTC)
              : now;

      dsl.update(PROPERTY_INSURANCES)
          .set(PROPERTY_INSURANCES.INSURANCE_TYPE, insurance.getInsuranceType().name())
          .set(PROPERTY_INSURANCES.PROVIDER, insurance.getProvider().orElse(null))
          .set(PROPERTY_INSURANCES.POLICY_NUMBER, insurance.getPolicyNumber().orElse(null))
          .set(
              PROPERTY_INSURANCES.COVERAGE_AMOUNT,
              insurance.getCoverageAmount().map(MoneyAmount::value).orElse(null))
          .set(
              PROPERTY_INSURANCES.COVERAGE_AMOUNT_CURRENCY,
              insurance.getCoverageAmount().map(MoneyAmount::currency).orElse(null))
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM, insurance.getAnnualPremium().value())
          .set(PROPERTY_INSURANCES.ANNUAL_PREMIUM_CURRENCY, insurance.getAnnualPremium().currency())
          .set(PROPERTY_INSURANCES.PAYMENT_FREQUENCY, insurance.getPaymentFrequency())
          .set(PROPERTY_INSURANCES.START_DATE, insurance.getStartDate().orElse(null))
          .set(PROPERTY_INSURANCES.END_DATE, insurance.getEndDate().orElse(null))
          .set(PROPERTY_INSURANCES.STATUS, insurance.getStatus().name())
          .set(PROPERTY_INSURANCES.NOTES, insurance.getNotes().orElse(null))
          .set(PROPERTY_INSURANCES.UPDATED_AT, updatedAt)
          .set(PROPERTY_INSURANCES.UPDATED_BY, insurance.getUpdatedBy())
          .where(
              PROPERTY_INSURANCES
                  .ID
                  .eq(insurance.getId())
                  .and(PROPERTY_INSURANCES.TEAM_ID.eq(insurance.getTeamId())))
          .execute();

      insurance.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return insurance;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTY_INSURANCES)
        .set(PROPERTY_INSURANCES.DELETED_AT, now)
        .where(PROPERTY_INSURANCES.ID.eq(id).and(PROPERTY_INSURANCES.TEAM_ID.eq(teamId)))
        .execute();
  }
}
