package com.buurman.dto.request;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.ReminderTone;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record BulkSendPaymentRemindersRequest(
    @NotEmpty(message = "At least one payment identifier is required") @Size(max = 200, message = "At most 200 reminders can be sent at once") List<PaymentIdentifier> identifiers,
    @Size(max = 1000, message = "Notes must be at most 1000 characters") Optional<String> notes,
    Optional<ReminderTone> tone) {}
