package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_FINANCINGS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyFinancing;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PropertyFinancingRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyFinancingRepository {

  private final DSLContext dsl;
  private final PropertyFinancingRecordMapper mapper;
  private final Clock clock;

  public Optional<PropertyFinancing> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(PROPERTY_FINANCINGS)
        .where(
            PROPERTY_FINANCINGS
                .IDENTIFIER
                .eq(identifier)
                .and(PROPERTY_FINANCINGS.TEAM_ID.eq(teamId))
                .and(PROPERTY_FINANCINGS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public PropertyFinancing getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Property financing not found"));
  }

  public Optional<PropertyFinancing> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(PROPERTY_FINANCINGS)
        .where(
            PROPERTY_FINANCINGS
                .ID
                .eq(id)
                .and(PROPERTY_FINANCINGS.TEAM_ID.eq(teamId))
                .and(PROPERTY_FINANCINGS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public List<PropertyFinancing> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_FINANCINGS)
        .where(
            PROPERTY_FINANCINGS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_FINANCINGS.TEAM_ID.eq(teamId))
                .and(PROPERTY_FINANCINGS.DELETED_AT.isNull()))
        .orderBy(PROPERTY_FINANCINGS.START_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<PropertyFinancing> findActiveByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_FINANCINGS)
        .where(
            PROPERTY_FINANCINGS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_FINANCINGS.TEAM_ID.eq(teamId))
                .and(PROPERTY_FINANCINGS.STATUS.eq(PropertyFinancing.FinancingStatus.ACTIVE.name()))
                .and(PROPERTY_FINANCINGS.DELETED_AT.isNull()))
        .orderBy(PROPERTY_FINANCINGS.START_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public PropertyFinancing save(PropertyFinancing financing) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (financing.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          financing.getCreatedAt() != null
              ? LocalDateTime.ofInstant(financing.getCreatedAt(), UTC)
              : now;
      LocalDateTime updatedAt =
          financing.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(financing.getUpdatedAt(), UTC)
              : now;

      dsl.insertInto(PROPERTY_FINANCINGS)
          .set(PROPERTY_FINANCINGS.ID, id)
          .set(PROPERTY_FINANCINGS.IDENTIFIER, financing.getIdentifier().orElseThrow())
          .set(PROPERTY_FINANCINGS.PROPERTY_ID, financing.getPropertyId())
          .set(PROPERTY_FINANCINGS.TEAM_ID, financing.getTeamId())
          .set(PROPERTY_FINANCINGS.FINANCING_TYPE, financing.getFinancingType().name())
          .set(PROPERTY_FINANCINGS.RATE_TYPE, financing.getRateType().name())
          .set(PROPERTY_FINANCINGS.LENDER_NAME, financing.getLenderName().orElse(null))
          .set(PROPERTY_FINANCINGS.LOAN_NUMBER, financing.getLoanNumber().orElse(null))
          .set(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT, financing.getOriginalAmount().value())
          .set(
              PROPERTY_FINANCINGS.ORIGINAL_AMOUNT_CURRENCY,
              financing.getOriginalAmount().currency())
          .set(PROPERTY_FINANCINGS.CURRENT_BALANCE, financing.getCurrentBalance().orElse(null))
          .set(
              PROPERTY_FINANCINGS.CURRENT_BALANCE_CURRENCY,
              financing.getCurrentBalance().isPresent()
                  ? financing.getOriginalAmount().currency()
                  : null)
          .set(PROPERTY_FINANCINGS.INTEREST_RATE, financing.getInterestRate().orElse(null))
          .set(PROPERTY_FINANCINGS.MONTHLY_PAYMENT, financing.getMonthlyPayment().orElse(null))
          .set(
              PROPERTY_FINANCINGS.MONTHLY_PAYMENT_CURRENCY,
              financing.getMonthlyPayment().isPresent()
                  ? financing.getOriginalAmount().currency()
                  : null)
          .set(PROPERTY_FINANCINGS.PAYMENT_VARIABLE, financing.isPaymentVariable())
          .set(PROPERTY_FINANCINGS.START_DATE, financing.getStartDate())
          .set(PROPERTY_FINANCINGS.END_DATE, financing.getEndDate().orElse(null))
          .set(PROPERTY_FINANCINGS.TERM_MONTHS, financing.getTermMonths().orElse(null))
          .set(PROPERTY_FINANCINGS.STATUS, financing.getStatus().name())
          .set(PROPERTY_FINANCINGS.NOTES, financing.getNotes().orElse(null))
          .set(PROPERTY_FINANCINGS.CREATED_AT, createdAt)
          .set(PROPERTY_FINANCINGS.UPDATED_AT, updatedAt)
          .set(PROPERTY_FINANCINGS.CREATED_BY, financing.getCreatedBy())
          .set(PROPERTY_FINANCINGS.UPDATED_BY, financing.getUpdatedBy())
          .execute();

      financing.setId(id);
      financing.setCreatedAt(createdAt.toInstant(UTC));
      financing.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // Update
      LocalDateTime updatedAt =
          financing.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(financing.getUpdatedAt(), UTC)
              : now;

      dsl.update(PROPERTY_FINANCINGS)
          .set(PROPERTY_FINANCINGS.FINANCING_TYPE, financing.getFinancingType().name())
          .set(PROPERTY_FINANCINGS.RATE_TYPE, financing.getRateType().name())
          .set(PROPERTY_FINANCINGS.LENDER_NAME, financing.getLenderName().orElse(null))
          .set(PROPERTY_FINANCINGS.LOAN_NUMBER, financing.getLoanNumber().orElse(null))
          .set(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT, financing.getOriginalAmount().value())
          .set(
              PROPERTY_FINANCINGS.ORIGINAL_AMOUNT_CURRENCY,
              financing.getOriginalAmount().currency())
          .set(PROPERTY_FINANCINGS.CURRENT_BALANCE, financing.getCurrentBalance().orElse(null))
          .set(
              PROPERTY_FINANCINGS.CURRENT_BALANCE_CURRENCY,
              financing.getCurrentBalance().isPresent()
                  ? financing.getOriginalAmount().currency()
                  : null)
          .set(PROPERTY_FINANCINGS.INTEREST_RATE, financing.getInterestRate().orElse(null))
          .set(PROPERTY_FINANCINGS.MONTHLY_PAYMENT, financing.getMonthlyPayment().orElse(null))
          .set(
              PROPERTY_FINANCINGS.MONTHLY_PAYMENT_CURRENCY,
              financing.getMonthlyPayment().isPresent()
                  ? financing.getOriginalAmount().currency()
                  : null)
          .set(PROPERTY_FINANCINGS.PAYMENT_VARIABLE, financing.isPaymentVariable())
          .set(PROPERTY_FINANCINGS.START_DATE, financing.getStartDate())
          .set(PROPERTY_FINANCINGS.END_DATE, financing.getEndDate().orElse(null))
          .set(PROPERTY_FINANCINGS.TERM_MONTHS, financing.getTermMonths().orElse(null))
          .set(PROPERTY_FINANCINGS.STATUS, financing.getStatus().name())
          .set(PROPERTY_FINANCINGS.NOTES, financing.getNotes().orElse(null))
          .set(PROPERTY_FINANCINGS.UPDATED_AT, updatedAt)
          .set(PROPERTY_FINANCINGS.UPDATED_BY, financing.getUpdatedBy())
          .where(
              PROPERTY_FINANCINGS
                  .ID
                  .eq(financing.getId())
                  .and(PROPERTY_FINANCINGS.TEAM_ID.eq(financing.getTeamId())))
          .execute();

      financing.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return financing;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTY_FINANCINGS)
        .set(PROPERTY_FINANCINGS.DELETED_AT, now)
        .where(PROPERTY_FINANCINGS.ID.eq(id).and(PROPERTY_FINANCINGS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
