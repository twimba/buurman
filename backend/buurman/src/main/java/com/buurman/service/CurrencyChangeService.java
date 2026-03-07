package com.buurman.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.TeamPreferences;
import com.buurman.dto.request.CurrencyChangeRequest;
import com.buurman.dto.request.CurrencyChangeRequest.ChangeMode;
import com.buurman.dto.response.CurrencyChangeResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.repository.CurrencyChangeRepository;
import com.buurman.repository.TeamPreferencesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyChangeService {

  private final CurrencyChangeRepository currencyChangeRepository;
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
    String newCurrency = request.newCurrency().toUpperCase(Locale.ROOT);

    if (oldCurrency.equalsIgnoreCase(newCurrency)) {
      throw new BadRequestException("New currency is the same as the current team currency");
    }

    if (request.mode() == ChangeMode.CONVERT && request.conversionRate().isEmpty()) {
      throw new BadRequestException("Conversion rate is required for CONVERT mode");
    }

    // Only pass rate for CONVERT mode
    Optional<BigDecimal> conversionRate =
        request.mode() == ChangeMode.CONVERT ? request.conversionRate() : Optional.empty();

    // Update all financial tables
    int contracts =
        currencyChangeRepository.updateContractCurrencies(
            teamId, oldCurrency, newCurrency, conversionRate);
    int payments =
        currencyChangeRepository.updatePaymentCurrencies(
            teamId, oldCurrency, newCurrency, conversionRate);
    int expenses =
        currencyChangeRepository.updateExpenseCurrencies(
            teamId, oldCurrency, newCurrency, conversionRate);
    int financials =
        currencyChangeRepository.updatePropertyFinancials(
            teamId, oldCurrency, newCurrency, conversionRate);

    // Update team preferences
    prefs.setDefaultCurrency(newCurrency);
    teamPreferencesRepository.save(prefs);

    // Log the change
    int totalAffected = contracts + payments + expenses + financials;
    currencyChangeRepository.logCurrencyChange(
        teamId,
        oldCurrency,
        newCurrency,
        request.mode().name(),
        conversionRate,
        contracts,
        payments,
        expenses,
        financials,
        userId);

    log.info(
        "Currency changed for team {} from {} to {} (mode={}, rate={}, affected={})",
        teamId,
        oldCurrency,
        newCurrency,
        request.mode(),
        conversionRate,
        totalAffected);

    return new CurrencyChangeResponse(
        oldCurrency,
        newCurrency,
        request.mode().name(),
        conversionRate,
        contracts,
        payments,
        expenses,
        financials);
  }
}
