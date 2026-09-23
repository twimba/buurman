package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record SendPaymentReminderRequest(
    @Size(max = 1000, message = "Notes must be at most 1000 characters") Optional<String> notes) {

  public static SendPaymentReminderRequest empty() {
    return new SendPaymentReminderRequest(Optional.empty());
  }
}
