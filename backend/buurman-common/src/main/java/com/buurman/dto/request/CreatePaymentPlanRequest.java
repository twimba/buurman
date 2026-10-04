package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.PaymentPlan;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record CreatePaymentPlanRequest(
    @NotEmpty(message = "At least one payment to cover is required") @Size(max = 50, message = "At most 50 payments can be covered by one plan") List<PaymentIdentifier> paymentIdentifiers,
    @NotNull(message = "Instalment count is required") @Min(value = 1, message = "At least 1 instalment") @Max(value = 36, message = "At most 36 instalments") Integer instalmentCount,
    @NotNull(message = "Start date is required") LocalDate startDate,
    Optional<PaymentPlan.Frequency> frequency,
    @Size(max = 2000) Optional<String> notes,
    // Pause tenant reminders on the contract until the last instalment is due. Default true.
    Optional<Boolean> pauseReminders) {}
