package com.buurman.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BulkCreatePaymentsRequest(
    @NotNull(message = "Items are required") @Size(min = 1, max = 100, message = "Between 1 and 100 items allowed") List<CreatePaymentRequest> items) {}
