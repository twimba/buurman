package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.CURRENCY_CHANGE_LOG;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.FINANCING_PAYMENTS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTY_ACQUISITIONS;
import static com.buurman.jooq.generated.Tables.PROPERTY_FEES;
import static com.buurman.jooq.generated.Tables.PROPERTY_FINANCINGS;
import static com.buurman.jooq.generated.Tables.PROPERTY_INSURANCES;
import static com.buurman.jooq.generated.Tables.PROPERTY_TAXES;
import static com.buurman.jooq.generated.Tables.PROPERTY_VALUATIONS;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CurrencyChangeRepository {

  /** Rounding precision matching {@link com.buurman.config.jooq.MoneyMinorUnitConverter#SCALE}. */
  private static final int MINOR_UNIT_SCALE = 2;

  private final DSLContext dsl;

  public int updateContractCurrencies(
      UUID teamId, String oldCurrency, String newCurrency, Optional<BigDecimal> conversionRate) {

    var update = dsl.update(CONTRACTS).set(CONTRACTS.RENT_AMOUNT_CURRENCY, newCurrency);

    if (conversionRate.isPresent()) {
      // Use DSL.val(rate) to avoid the MoneyMinorUnitConverter being applied to the rate.
      // Without this, JOOQ converts 0.1 → 10 (minor units) via the field's converter.
      Field<BigDecimal> rate = DSL.val(conversionRate.get());
      update =
          update.set(
              CONTRACTS.RENT_AMOUNT,
              DSL.round(CONTRACTS.RENT_AMOUNT.mul(rate), MINOR_UNIT_SCALE)
                  .cast(CONTRACTS.RENT_AMOUNT.getDataType()));
      update =
          update.set(
              CONTRACTS.DEPOSIT_AMOUNT,
              DSL.when(
                      CONTRACTS.DEPOSIT_AMOUNT.isNotNull(),
                      DSL.round(CONTRACTS.DEPOSIT_AMOUNT.mul(rate), MINOR_UNIT_SCALE)
                          .cast(CONTRACTS.DEPOSIT_AMOUNT.getDataType()))
                  .otherwise((BigDecimal) null));
      update =
          update.set(
              CONTRACTS.SECURITY_DEPOSIT,
              DSL.when(
                      CONTRACTS.SECURITY_DEPOSIT.isNotNull(),
                      DSL.round(CONTRACTS.SECURITY_DEPOSIT.mul(rate), MINOR_UNIT_SCALE)
                          .cast(CONTRACTS.SECURITY_DEPOSIT.getDataType()))
                  .otherwise((BigDecimal) null));
    }

    // Also update deposit/security deposit currency where they match old currency
    update =
        update.set(
            CONTRACTS.DEPOSIT_AMOUNT_CURRENCY,
            DSL.when(CONTRACTS.DEPOSIT_AMOUNT_CURRENCY.eq(oldCurrency), newCurrency)
                .otherwise(CONTRACTS.DEPOSIT_AMOUNT_CURRENCY));
    update =
        update.set(
            CONTRACTS.SECURITY_DEPOSIT_CURRENCY,
            DSL.when(CONTRACTS.SECURITY_DEPOSIT_CURRENCY.eq(oldCurrency), newCurrency)
                .otherwise(CONTRACTS.SECURITY_DEPOSIT_CURRENCY));

    return update
        .where(CONTRACTS.TEAM_ID.eq(teamId))
        .and(CONTRACTS.RENT_AMOUNT_CURRENCY.eq(oldCurrency))
        .and(CONTRACTS.DELETED_AT.isNull())
        .execute();
  }

  public int updatePaymentCurrencies(
      UUID teamId, String oldCurrency, String newCurrency, Optional<BigDecimal> conversionRate) {

    return updateSimpleCurrency(
        PAYMENTS,
        PAYMENTS.TEAM_ID,
        PAYMENTS.CURRENCY,
        teamId,
        oldCurrency,
        newCurrency,
        conversionRate,
        Optional.of(PAYMENTS.AMOUNT));
  }

  public int updateExpenseCurrencies(
      UUID teamId, String oldCurrency, String newCurrency, Optional<BigDecimal> conversionRate) {

    return updateSimpleCurrency(
        EXPENSES,
        EXPENSES.TEAM_ID,
        EXPENSES.CURRENCY,
        teamId,
        oldCurrency,
        newCurrency,
        conversionRate,
        Optional.of(EXPENSES.AMOUNT));
  }

  public int updatePropertyFinancials(
      UUID teamId, String oldCurrency, String newCurrency, Optional<BigDecimal> conversionRate) {

    int count = 0;

    // Property acquisitions
    var acqUpdate =
        dsl.update(PROPERTY_ACQUISITIONS)
            .set(
                PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY,
                DSL.when(PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY.eq(oldCurrency), newCurrency)
                    .otherwise(PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY))
            .set(
                PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY,
                DSL.when(PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY.eq(oldCurrency), newCurrency)
                    .otherwise(PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY))
            .set(
                PROPERTY_ACQUISITIONS.RENOVATION_COSTS_CURRENCY,
                DSL.when(
                        PROPERTY_ACQUISITIONS.RENOVATION_COSTS_CURRENCY.eq(oldCurrency),
                        newCurrency)
                    .otherwise(PROPERTY_ACQUISITIONS.RENOVATION_COSTS_CURRENCY))
            .set(
                PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY,
                DSL.when(PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY.eq(oldCurrency), newCurrency)
                    .otherwise(PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY));

    if (conversionRate.isPresent()) {
      Field<BigDecimal> rate = DSL.val(conversionRate.get());
      acqUpdate =
          acqUpdate
              .set(
                  PROPERTY_ACQUISITIONS.PURCHASE_PRICE,
                  DSL.when(
                          PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY.eq(oldCurrency),
                          DSL.round(
                                  PROPERTY_ACQUISITIONS.PURCHASE_PRICE.mul(rate), MINOR_UNIT_SCALE)
                              .cast(PROPERTY_ACQUISITIONS.PURCHASE_PRICE.getDataType()))
                      .otherwise(PROPERTY_ACQUISITIONS.PURCHASE_PRICE))
              .set(
                  PROPERTY_ACQUISITIONS.CLOSING_COSTS,
                  DSL.when(
                          PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY.eq(oldCurrency),
                          DSL.round(PROPERTY_ACQUISITIONS.CLOSING_COSTS.mul(rate), MINOR_UNIT_SCALE)
                              .cast(PROPERTY_ACQUISITIONS.CLOSING_COSTS.getDataType()))
                      .otherwise(PROPERTY_ACQUISITIONS.CLOSING_COSTS))
              .set(
                  PROPERTY_ACQUISITIONS.RENOVATION_COSTS,
                  DSL.when(
                          PROPERTY_ACQUISITIONS.RENOVATION_COSTS_CURRENCY.eq(oldCurrency),
                          DSL.round(
                                  PROPERTY_ACQUISITIONS.RENOVATION_COSTS.mul(rate),
                                  MINOR_UNIT_SCALE)
                              .cast(PROPERTY_ACQUISITIONS.RENOVATION_COSTS.getDataType()))
                      .otherwise(PROPERTY_ACQUISITIONS.RENOVATION_COSTS))
              .set(
                  PROPERTY_ACQUISITIONS.LAND_VALUE,
                  DSL.when(
                          PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY.eq(oldCurrency),
                          DSL.round(PROPERTY_ACQUISITIONS.LAND_VALUE.mul(rate), MINOR_UNIT_SCALE)
                              .cast(PROPERTY_ACQUISITIONS.LAND_VALUE.getDataType()))
                      .otherwise(PROPERTY_ACQUISITIONS.LAND_VALUE));
    }

    count +=
        acqUpdate
            .where(PROPERTY_ACQUISITIONS.TEAM_ID.eq(teamId))
            .and(PROPERTY_ACQUISITIONS.DELETED_AT.isNull())
            .execute();

    // Property financings
    count +=
        updateSimpleCurrency(
            PROPERTY_FINANCINGS,
            PROPERTY_FINANCINGS.TEAM_ID,
            PROPERTY_FINANCINGS.ORIGINAL_AMOUNT_CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            conversionRate,
            Optional.of(PROPERTY_FINANCINGS.ORIGINAL_AMOUNT));

    // Financing payments
    count +=
        updateSimpleCurrency(
            FINANCING_PAYMENTS,
            FINANCING_PAYMENTS.TEAM_ID,
            FINANCING_PAYMENTS.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            conversionRate,
            Optional.of(FINANCING_PAYMENTS.TOTAL_AMOUNT));

    // Property taxes
    count +=
        updateSimpleCurrency(
            PROPERTY_TAXES,
            PROPERTY_TAXES.TEAM_ID,
            PROPERTY_TAXES.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            conversionRate,
            Optional.of(PROPERTY_TAXES.ANNUAL_AMOUNT));

    // Property insurances
    count +=
        updateSimpleCurrency(
            PROPERTY_INSURANCES,
            PROPERTY_INSURANCES.TEAM_ID,
            PROPERTY_INSURANCES.ANNUAL_PREMIUM_CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            conversionRate,
            Optional.of(PROPERTY_INSURANCES.ANNUAL_PREMIUM));

    // Property fees
    count +=
        updateSimpleCurrency(
            PROPERTY_FEES,
            PROPERTY_FEES.TEAM_ID,
            PROPERTY_FEES.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            conversionRate,
            Optional.of(PROPERTY_FEES.ANNUAL_AMOUNT));

    // Property valuations
    count +=
        updateSimpleCurrency(
            PROPERTY_VALUATIONS,
            PROPERTY_VALUATIONS.TEAM_ID,
            PROPERTY_VALUATIONS.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            conversionRate,
            Optional.of(PROPERTY_VALUATIONS.AMOUNT));

    return count;
  }

  public void logCurrencyChange(
      UUID teamId,
      String oldCurrency,
      String newCurrency,
      String changeMode,
      Optional<BigDecimal> conversionRate,
      int contracts,
      int payments,
      int expenses,
      int financials,
      UUID userId) {

    dsl.insertInto(CURRENCY_CHANGE_LOG)
        .set(CURRENCY_CHANGE_LOG.TEAM_ID, teamId)
        .set(CURRENCY_CHANGE_LOG.OLD_CURRENCY, oldCurrency)
        .set(CURRENCY_CHANGE_LOG.NEW_CURRENCY, newCurrency)
        .set(CURRENCY_CHANGE_LOG.CHANGE_MODE, changeMode)
        .set(CURRENCY_CHANGE_LOG.CONVERSION_RATE, conversionRate.orElse(null))
        .set(CURRENCY_CHANGE_LOG.AFFECTED_CONTRACTS, contracts)
        .set(CURRENCY_CHANGE_LOG.AFFECTED_PAYMENTS, payments)
        .set(CURRENCY_CHANGE_LOG.AFFECTED_EXPENSES, expenses)
        .set(CURRENCY_CHANGE_LOG.AFFECTED_FINANCIALS, financials)
        .set(CURRENCY_CHANGE_LOG.CHANGED_BY, userId)
        .execute();
  }

  @SuppressWarnings("unchecked")
  private <R extends org.jooq.Record> int updateSimpleCurrency(
      Table<R> table,
      Field<UUID> teamIdField,
      Field<String> currencyField,
      UUID teamId,
      String oldCurrency,
      String newCurrency,
      Optional<BigDecimal> conversionRate,
      Optional<Field<BigDecimal>> amountField) {

    var update = dsl.update(table).set(currencyField, newCurrency);

    if (conversionRate.isPresent() && amountField.isPresent()) {
      Field<BigDecimal> rate = DSL.val(conversionRate.get());
      Field<BigDecimal> field = amountField.get();
      update =
          update.set(field, DSL.round(field.mul(rate), MINOR_UNIT_SCALE).cast(field.getDataType()));
    }

    return update.where(teamIdField.eq(teamId)).and(currencyField.eq(oldCurrency)).execute();
  }
}
