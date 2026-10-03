package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

/**
 * A lease agreement cannot be produced for the contract's country (or the contract has none).
 * Mapped to 409 with a machine-readable {@code code} so clients can react without parsing text.
 */
@SkipTestCoverage
public class LeaseNotAvailableException extends BusinessRuleException {

  public static final String CODE_COUNTRY = "LEASE_NOT_AVAILABLE_FOR_COUNTRY";
  public static final String CODE_NO_COUNTRY = "LEASE_CONTRACT_HAS_NO_COUNTRY";

  private final String code;

  private LeaseNotAvailableException(String message, String code) {
    super(message);
    this.code = code;
  }

  public static LeaseNotAvailableException forCountry(String countryCode) {
    return new LeaseNotAvailableException(
        "No lease clause templates are configured for country " + countryCode, CODE_COUNTRY);
  }

  public static LeaseNotAvailableException noCountry() {
    return new LeaseNotAvailableException("Contract has no country code set", CODE_NO_COUNTRY);
  }

  public String getCode() {
    return code;
  }
}
