package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record BulkMarkPaidRequest(
    @NotEmpty(message = "At least one payment identifier is required") @Size(max = 200, message = "At most 200 payments can be marked paid at once") List<PaymentIdentifier> identifiers,
    @NotNull(message = "Payment date is required") LocalDate paymentDate,
    Optional<String> notes) {}
