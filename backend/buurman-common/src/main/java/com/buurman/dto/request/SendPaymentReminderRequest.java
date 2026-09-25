package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.ReminderTone;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record SendPaymentReminderRequest(
    @Size(max = 1000, message = "Notes must be at most 1000 characters") Optional<String> notes,
    /** Wording; FINAL also generates and attaches a formal notice PDF. Defaults by days overdue. */
    Optional<ReminderTone> tone) {

  public SendPaymentReminderRequest(Optional<String> notes) {
    this(notes, Optional.empty());
  }

  public static SendPaymentReminderRequest empty() {
    return new SendPaymentReminderRequest(Optional.empty(), Optional.empty());
  }
}
