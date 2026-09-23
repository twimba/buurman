package com.buurman.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One rung of a team's dunning ladder: send a reminder of the given tone once a payment is {@code
 * offsetDays} past its due date (negative = before the due date).
 */
public record PaymentReminderStep(
    @JsonProperty("offsetDays") int offsetDays,
    @JsonProperty("tone") ReminderTone tone,
    @JsonProperty("enabled") boolean enabled) {

  @JsonCreator
  public PaymentReminderStep {}
}
