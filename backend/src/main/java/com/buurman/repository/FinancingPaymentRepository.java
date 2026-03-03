package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.FINANCING_PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTY_FINANCINGS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.FinancingPayment;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.FinancingPaymentRecordMapper;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;
import com.buurman.domain.Ulid;

@Repository
@RequiredArgsConstructor
public class FinancingPaymentRepository {

  private final DSLContext dsl;
  private final FinancingPaymentRecordMapper mapper;
  private final Clock clock;

  public Optional<FinancingPayment> findByIdentifierAndTeamId(Ulid identifier, UUID teamId) {
    return dsl.selectFrom(FINANCING_PAYMENTS)
        .where(
            FINANCING_PAYMENTS
                .IDENTIFIER
                .eq(identifier)
                .and(FINANCING_PAYMENTS.TEAM_ID.eq(teamId))
                .and(FINANCING_PAYMENTS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public FinancingPayment getByIdentifierAndTeamId(Ulid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Financing payment not found"));
  }

  public Optional<FinancingPayment> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(FINANCING_PAYMENTS)
        .where(
            FINANCING_PAYMENTS
                .ID
                .eq(id)
                .and(FINANCING_PAYMENTS.TEAM_ID.eq(teamId))
                .and(FINANCING_PAYMENTS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public List<FinancingPayment> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(FINANCING_PAYMENTS)
        .where(
            FINANCING_PAYMENTS.FINANCING_ID.in(
                dsl.select(PROPERTY_FINANCINGS.ID)
                    .from(PROPERTY_FINANCINGS)
                    .where(
                        PROPERTY_FINANCINGS
                            .PROPERTY_ID
                            .eq(propertyId)
                            .and(PROPERTY_FINANCINGS.TEAM_ID.eq(teamId))
                            .and(PROPERTY_FINANCINGS.DELETED_AT.isNull()))))
        .and(FINANCING_PAYMENTS.TEAM_ID.eq(teamId))
        .and(FINANCING_PAYMENTS.DELETED_AT.isNull())
        .orderBy(FINANCING_PAYMENTS.PAYMENT_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<FinancingPayment> findByFinancingIdAndTeamId(UUID financingId, UUID teamId) {
    return dsl
        .selectFrom(FINANCING_PAYMENTS)
        .where(
            FINANCING_PAYMENTS
                .FINANCING_ID
                .eq(financingId)
                .and(FINANCING_PAYMENTS.TEAM_ID.eq(teamId))
                .and(FINANCING_PAYMENTS.DELETED_AT.isNull()))
        .orderBy(FINANCING_PAYMENTS.PAYMENT_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public FinancingPayment save(FinancingPayment payment) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (payment.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          payment.getCreatedAt() != null
              ? LocalDateTime.ofInstant(payment.getCreatedAt(), UTC)
              : now;
      LocalDateTime updatedAt =
          payment.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(payment.getUpdatedAt(), UTC)
              : now;

      String currency = payment.getCurrency();
      dsl.insertInto(FINANCING_PAYMENTS)
          .set(FINANCING_PAYMENTS.ID, id)
          .set(FINANCING_PAYMENTS.IDENTIFIER, payment.getIdentifier().orElseThrow())
          .set(FINANCING_PAYMENTS.FINANCING_ID, payment.getFinancingId())
          .set(FINANCING_PAYMENTS.TEAM_ID, payment.getTeamId())
          .set(FINANCING_PAYMENTS.PAYMENT_DATE, payment.getPaymentDate())
          .set(
              FINANCING_PAYMENTS.TOTAL_AMOUNT,
              CurrencyUtils.toMinorUnits(payment.getTotalAmount(), currency))
          .set(
              FINANCING_PAYMENTS.PRINCIPAL_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(payment.getPrincipalAmount().orElse(null), currency))
          .set(
              FINANCING_PAYMENTS.INTEREST_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(payment.getInterestAmount().orElse(null), currency))
          .set(
              FINANCING_PAYMENTS.ESCROW_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(payment.getEscrowAmount().orElse(null), currency))
          .set(
              FINANCING_PAYMENTS.EXTRA_PAYMENT,
              CurrencyUtils.toMinorUnitsOrNull(payment.getExtraPayment().orElse(null), currency))
          .set(FINANCING_PAYMENTS.CURRENCY, currency)
          .set(FINANCING_PAYMENTS.STATUS, payment.getStatus().name())
          .set(FINANCING_PAYMENTS.NOTES, payment.getNotes().orElse(null))
          .set(FINANCING_PAYMENTS.BALANCE_DEDUCTED, payment.isBalanceDeducted())
          .set(FINANCING_PAYMENTS.CREATED_AT, createdAt)
          .set(FINANCING_PAYMENTS.UPDATED_AT, updatedAt)
          .set(FINANCING_PAYMENTS.CREATED_BY, payment.getCreatedBy())
          .set(FINANCING_PAYMENTS.UPDATED_BY, payment.getUpdatedBy())
          .execute();

      payment.setId(id);
      payment.setCreatedAt(createdAt.toInstant(UTC));
      payment.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // Update
      LocalDateTime updatedAt =
          payment.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(payment.getUpdatedAt(), UTC)
              : now;

      String currency = payment.getCurrency();
      dsl.update(FINANCING_PAYMENTS)
          .set(FINANCING_PAYMENTS.PAYMENT_DATE, payment.getPaymentDate())
          .set(
              FINANCING_PAYMENTS.TOTAL_AMOUNT,
              CurrencyUtils.toMinorUnits(payment.getTotalAmount(), currency))
          .set(
              FINANCING_PAYMENTS.PRINCIPAL_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(payment.getPrincipalAmount().orElse(null), currency))
          .set(
              FINANCING_PAYMENTS.INTEREST_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(payment.getInterestAmount().orElse(null), currency))
          .set(
              FINANCING_PAYMENTS.ESCROW_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(payment.getEscrowAmount().orElse(null), currency))
          .set(
              FINANCING_PAYMENTS.EXTRA_PAYMENT,
              CurrencyUtils.toMinorUnitsOrNull(payment.getExtraPayment().orElse(null), currency))
          .set(FINANCING_PAYMENTS.CURRENCY, currency)
          .set(FINANCING_PAYMENTS.STATUS, payment.getStatus().name())
          .set(FINANCING_PAYMENTS.NOTES, payment.getNotes().orElse(null))
          .set(FINANCING_PAYMENTS.BALANCE_DEDUCTED, payment.isBalanceDeducted())
          .set(FINANCING_PAYMENTS.UPDATED_AT, updatedAt)
          .set(FINANCING_PAYMENTS.UPDATED_BY, payment.getUpdatedBy())
          .where(
              FINANCING_PAYMENTS
                  .ID
                  .eq(payment.getId())
                  .and(FINANCING_PAYMENTS.TEAM_ID.eq(payment.getTeamId())))
          .execute();

      payment.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return payment;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(FINANCING_PAYMENTS)
        .set(FINANCING_PAYMENTS.DELETED_AT, now)
        .where(FINANCING_PAYMENTS.ID.eq(id).and(FINANCING_PAYMENTS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
