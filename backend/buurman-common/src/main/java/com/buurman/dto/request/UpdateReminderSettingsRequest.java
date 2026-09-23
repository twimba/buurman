package com.buurman.dto.request;

import java.util.List;

import com.buurman.domain.ReminderTone;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record UpdateReminderSettingsRequest(
    @NotNull(message = "automaticRemindersEnabled is required") Boolean automaticRemindersEnabled,
    @NotNull(message = "steps is required") @Size(max = 10, message = "At most 10 reminder steps are allowed") List<@Valid StepRequest> steps) {

  public record StepRequest(
      @NotNull(message = "offsetDays is required") @Min(value = -30, message = "offsetDays must be at least -30") @Max(value = 365, message = "offsetDays must be at most 365") Integer offsetDays,
      @NotNull(message = "tone is required") ReminderTone tone,
      @NotNull(message = "enabled is required") Boolean enabled) {}
}
