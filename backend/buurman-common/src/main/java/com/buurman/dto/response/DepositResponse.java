package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Deposit;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record DepositResponse(
    Sid identifier,
    Sid contractIdentifier,
    BigDecimal amount,
    String currency,
    Optional<LocalDate> receivedDate,
    Optional<String> heldWhere,
    Deposit.DepositStatus status,
    Optional<LocalDate> returnDueDate,
    Optional<LocalDate> returnedDate,
    BigDecimal returnedAmount,
    BigDecimal deductionsTotal,
    /** amount minus deductions minus what was already returned. */
    BigDecimal refundable,
    Optional<String> notes,
    List<DepositDeductionResponse> deductions,
    Instant createdAt,
    Optional<Instant> updatedAt) {

  public record DepositDeductionResponse(
      Sid identifier, BigDecimal amount, String currency, String reason, LocalDate deductionDate) {}
}
