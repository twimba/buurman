package com.buurman.dto.request;

import java.util.List;

import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record BulkCancelPaymentsRequest(
    @NotEmpty(message = "At least one payment identifier is required") @Size(max = 200, message = "At most 200 payments can be cancelled at once") List<PaymentIdentifier> identifiers,
    @NotBlank(message = "A reason is required") @Size(max = 1000) String reason) {}
