package com.buurman.util;

/**
 * Masks PII (emails, phone numbers, secret codes) before it reaches application logs. Logs flow
 * into the backoffice "Live tail" ring buffer, which any backoffice admin can read across tenants,
 * so raw recipient emails/phones and invitation codes must never be logged verbatim.
 */
public final class LogMasking {

  private LogMasking() {}

  /** {@code john.doe@example.com -> j***@example.com}; null/blank -> "(none)". */
  public static String email(String email) {
    if (email == null || email.isBlank()) {
      return "(none)";
    }
    int at = email.indexOf('@');
    if (at <= 0) {
      return "***";
    }
    return email.charAt(0) + "***" + email.substring(at);
  }

  /** Keep only the last 4 digits: {@code +31612345678 -> ***5678}; null/blank -> "(none)". */
  public static String phone(String phone) {
    if (phone == null || phone.isBlank()) {
      return "(none)";
    }
    String trimmed = phone.trim();
    if (trimmed.length() <= 4) {
      return "***";
    }
    return "***" + trimmed.substring(trimmed.length() - 4);
  }

  /**
   * Reveal only the first 4 chars of a secret/code: {@code ABCDEFGH -> ABCD…}; null -> "(none)".
   */
  public static String secret(String value) {
    if (value == null || value.isBlank()) {
      return "(none)";
    }
    return value.length() <= 4 ? "***" : value.substring(0, 4) + "…";
  }
}
