package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record MarkPaidRequest(
    @NotNull(message = "Payment date is required") LocalDate paymentDate, Optional<String> notes) {}
