package com.buurman.service;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.buurman.exception.BadRequestException;

import lombok.RequiredArgsConstructor;

/**
 * Enforces that all financial data uses the team's configured currency. Provides a single
 * validation point called by all financial services on create/update operations.
 */
@Service
@RequiredArgsConstructor
public class CurrencyEnforcementService {

  private final TeamService teamService;

  /**
   * Validates that the given currency matches the team's configured currency. Throws a 400 Bad
   * Request if they don't match.
   *
   * @param currency the currency to validate (null or blank values are silently accepted — they
   *     will be defaulted to the team currency by the caller)
   * @param teamId the team whose currency to check against
   */
  public void validateCurrency(@Nullable String currency, UUID teamId) {
    if (currency == null || currency.isBlank()) {
      return;
    }
    String teamCurrency = teamService.getDefaultCurrency(teamId);
    if (!currency.equalsIgnoreCase(teamCurrency)) {
      throw new BadRequestException(
          String.format(
              "Currency mismatch: '%s' does not match team currency '%s'. "
                  + "All financial data must use the team's configured currency.",
              currency, teamCurrency));
    }
  }

  /** Returns the team's configured currency code. */
  public String getTeamCurrency(UUID teamId) {
    return teamService.getDefaultCurrency(teamId);
  }
}
