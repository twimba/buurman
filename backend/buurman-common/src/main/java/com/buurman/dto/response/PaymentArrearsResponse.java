package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

/**
 * Team-wide arrears picture: outstanding money past its due date, aged into buckets and grouped by
 * the tenant who owes it. Amounts are in major units of the team currency.
 */
@SkipTestCoverage
public record PaymentArrearsResponse(
    Optional<String> currency,
    BigDecimal totalOutstanding,
    int paymentCount,
    int contactCount,
    int oldestDaysOverdue,
    List<AgeingBucket> buckets,
    List<ContactArrears> contacts) {

  /** One ageing band, e.g. 1-30 days. {@code toDays} is empty for the open-ended last band. */
  public record AgeingBucket(
      String key, int fromDays, Optional<Integer> toDays, BigDecimal amount, int count) {}

  /** Everything one tenant owes past due, oldest payment first. */
  public record ContactArrears(
      Optional<ContactSummary> contact,
      Optional<PropertySummary> property,
      Optional<Sid> contractIdentifier,
      BigDecimal outstanding,
      int paymentCount,
      LocalDate oldestDueDate,
      int daysOverdue,
      Optional<Instant> lastReminderAt,
      int reminderCount,
      List<Sid> paymentIdentifiers,
      boolean remindersEnabled) {}
}
