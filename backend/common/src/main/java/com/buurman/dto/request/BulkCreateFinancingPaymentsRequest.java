package com.buurman.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.buurman.util.Generated;

@Generated
public record BulkCreateFinancingPaymentsRequest(
    @NotNull(message = "Items are required") @Size(min = 1, max = 1000, message = "Between 1 and 1000 items allowed") List<CreateFinancingPaymentRequest> items) {}
