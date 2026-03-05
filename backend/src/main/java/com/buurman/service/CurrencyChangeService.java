package com.buurman.service;

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
import org.jspecify.annotations.Nullable;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.TeamPreferences;
import com.buurman.dto.request.CurrencyChangeRequest;
import com.buurman.dto.request.CurrencyChangeRequest.ChangeMode;
import com.buurman.dto.response.CurrencyChangeResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.TeamPreferencesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyChangeService {

  private final DSLContext dsl;
  private final TeamPreferencesRepository teamPreferencesRepository;

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public CurrencyChangeResponse changeCurrency(
      UUID teamId, CurrencyChangeRequest request, UUID userId) {

    // Validate ISO 4217 currency code
    try {
      java.util.Currency.getInstance(request.newCurrency());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid ISO 4217 currency code: " + request.newCurrency());
    }

    TeamPreferences prefs = teamPreferencesRepository.getByTeamId(teamId);
    String oldCurrency = prefs.getDefaultCurrency();
    String newCurrency = request.newCurrency().toUpperCase();

    if (oldCurrency.equalsIgnoreCase(newCurrency)) {
      throw new BadRequestException("New currency is the same as the current team currency");
    }

    if (request.mode() == ChangeMode.CONVERT && request.conversionRate().isEmpty()) {
      throw new BadRequestException("Conversion rate is required for CONVERT mode");
    }

    @Nullable BigDecimal rate = request.conversionRate().orElse(null);
    boolean isConvert = request.mode() == ChangeMode.CONVERT;

    // Update all financial tables
    int contracts = updateContractCurrencies(teamId, oldCurrency, newCurrency, rate, isConvert);
    int payments =
        updateSimpleCurrency(
            PAYMENTS,
            PAYMENTS.TEAM_ID,
            PAYMENTS.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            isConvert ? rate : null,
            PAYMENTS.AMOUNT);
    int expenses =
        updateSimpleCurrency(
            EXPENSES,
            EXPENSES.TEAM_ID,
            EXPENSES.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            isConvert ? rate : null,
            EXPENSES.AMOUNT);
    int financials = updatePropertyFinancials(teamId, oldCurrency, newCurrency, rate, isConvert);

    // Update team preferences
    prefs.setDefaultCurrency(newCurrency);
    teamPreferencesRepository.save(prefs);

    // Log the change
    int totalAffected = contracts + payments + expenses + financials;
    dsl.insertInto(CURRENCY_CHANGE_LOG)
        .set(CURRENCY_CHANGE_LOG.TEAM_ID, teamId)
        .set(CURRENCY_CHANGE_LOG.OLD_CURRENCY, oldCurrency)
        .set(CURRENCY_CHANGE_LOG.NEW_CURRENCY, newCurrency)
        .set(CURRENCY_CHANGE_LOG.CHANGE_MODE, request.mode().name())
        .set(CURRENCY_CHANGE_LOG.CONVERSION_RATE, rate)
        .set(CURRENCY_CHANGE_LOG.AFFECTED_CONTRACTS, contracts)
        .set(CURRENCY_CHANGE_LOG.AFFECTED_PAYMENTS, payments)
        .set(CURRENCY_CHANGE_LOG.AFFECTED_EXPENSES, expenses)
        .set(CURRENCY_CHANGE_LOG.AFFECTED_FINANCIALS, financials)
        .set(CURRENCY_CHANGE_LOG.CHANGED_BY, userId)
        .execute();

    log.info(
        "Currency changed for team {} from {} to {} (mode={}, rate={}, affected={})",
        teamId,
        oldCurrency,
        newCurrency,
        request.mode(),
        rate,
        totalAffected);

    return new CurrencyChangeResponse(
        oldCurrency,
        newCurrency,
        request.mode().name(),
        Optional.ofNullable(rate),
        contracts,
        payments,
        expenses,
        financials);
  }

  private int updateContractCurrencies(
      UUID teamId, String oldCurrency, String newCurrency, @Nullable BigDecimal rate, boolean isConvert) {

    var update = dsl.update(CONTRACTS).set(CONTRACTS.RENT_AMOUNT_CURRENCY, newCurrency);

    if (isConvert && rate != null) {
      update =
          update.set(
              CONTRACTS.RENT_AMOUNT,
              CONTRACTS.RENT_AMOUNT.mul(rate).cast(CONTRACTS.RENT_AMOUNT.getDataType()));
      update =
          update.set(
              CONTRACTS.DEPOSIT_AMOUNT,
              DSL.when(
                      CONTRACTS.DEPOSIT_AMOUNT.isNotNull(),
                      CONTRACTS
                          .DEPOSIT_AMOUNT
                          .mul(rate)
                          .cast(CONTRACTS.DEPOSIT_AMOUNT.getDataType()))
                  .otherwise((BigDecimal) null));
      update =
          update.set(
              CONTRACTS.SECURITY_DEPOSIT,
              DSL.when(
                      CONTRACTS.SECURITY_DEPOSIT.isNotNull(),
                      CONTRACTS
                          .SECURITY_DEPOSIT
                          .mul(rate)
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

  @SuppressWarnings("unchecked")
  private <R extends org.jooq.Record> int updateSimpleCurrency(
      Table<R> table,
      Field<UUID> teamIdField,
      Field<String> currencyField,
      UUID teamId,
      String oldCurrency,
      String newCurrency,
      @Nullable BigDecimal rate,
      @Nullable Field<BigDecimal> amountField) {

    var update = dsl.update(table).set(currencyField, newCurrency);

    if (rate != null && amountField != null) {
      update = update.set(amountField, amountField.mul(rate).cast(amountField.getDataType()));
    }

    return update.where(teamIdField.eq(teamId)).and(currencyField.eq(oldCurrency)).execute();
  }

  private int updatePropertyFinancials(
      UUID teamId, String oldCurrency, String newCurrency, @Nullable BigDecimal rate, boolean isConvert) {

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

    if (isConvert && rate != null) {
      acqUpdate =
          acqUpdate
              .set(
                  PROPERTY_ACQUISITIONS.PURCHASE_PRICE,
                  DSL.when(
                          PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY.eq(oldCurrency),
                          PROPERTY_ACQUISITIONS
                              .PURCHASE_PRICE
                              .mul(rate)
                              .cast(PROPERTY_ACQUISITIONS.PURCHASE_PRICE.getDataType()))
                      .otherwise(PROPERTY_ACQUISITIONS.PURCHASE_PRICE))
              .set(
                  PROPERTY_ACQUISITIONS.CLOSING_COSTS,
                  DSL.when(
                          PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY.eq(oldCurrency),
                          PROPERTY_ACQUISITIONS
                              .CLOSING_COSTS
                              .mul(rate)
                              .cast(PROPERTY_ACQUISITIONS.CLOSING_COSTS.getDataType()))
                      .otherwise(PROPERTY_ACQUISITIONS.CLOSING_COSTS))
              .set(
                  PROPERTY_ACQUISITIONS.RENOVATION_COSTS,
                  DSL.when(
                          PROPERTY_ACQUISITIONS.RENOVATION_COSTS_CURRENCY.eq(oldCurrency),
                          PROPERTY_ACQUISITIONS
                              .RENOVATION_COSTS
                              .mul(rate)
                              .cast(PROPERTY_ACQUISITIONS.RENOVATION_COSTS.getDataType()))
                      .otherwise(PROPERTY_ACQUISITIONS.RENOVATION_COSTS))
              .set(
                  PROPERTY_ACQUISITIONS.LAND_VALUE,
                  DSL.when(
                          PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY.eq(oldCurrency),
                          PROPERTY_ACQUISITIONS
                              .LAND_VALUE
                              .mul(rate)
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
            isConvert ? rate : null,
            PROPERTY_FINANCINGS.ORIGINAL_AMOUNT);

    // Financing payments
    count +=
        updateSimpleCurrency(
            FINANCING_PAYMENTS,
            FINANCING_PAYMENTS.TEAM_ID,
            FINANCING_PAYMENTS.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            isConvert ? rate : null,
            FINANCING_PAYMENTS.TOTAL_AMOUNT);

    // Property taxes
    count +=
        updateSimpleCurrency(
            PROPERTY_TAXES,
            PROPERTY_TAXES.TEAM_ID,
            PROPERTY_TAXES.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            isConvert ? rate : null,
            PROPERTY_TAXES.ANNUAL_AMOUNT);

    // Property insurances
    count +=
        updateSimpleCurrency(
            PROPERTY_INSURANCES,
            PROPERTY_INSURANCES.TEAM_ID,
            PROPERTY_INSURANCES.ANNUAL_PREMIUM_CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            isConvert ? rate : null,
            PROPERTY_INSURANCES.ANNUAL_PREMIUM);

    // Property fees
    count +=
        updateSimpleCurrency(
            PROPERTY_FEES,
            PROPERTY_FEES.TEAM_ID,
            PROPERTY_FEES.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            isConvert ? rate : null,
            PROPERTY_FEES.ANNUAL_AMOUNT);

    // Property valuations
    count +=
        updateSimpleCurrency(
            PROPERTY_VALUATIONS,
            PROPERTY_VALUATIONS.TEAM_ID,
            PROPERTY_VALUATIONS.CURRENCY,
            teamId,
            oldCurrency,
            newCurrency,
            isConvert ? rate : null,
            PROPERTY_VALUATIONS.AMOUNT);

    return count;
  }
}
