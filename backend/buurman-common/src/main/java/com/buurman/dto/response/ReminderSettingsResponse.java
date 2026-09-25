package com.buurman.dto.response;

import java.util.List;

import com.buurman.domain.PaymentReminderStep;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ReminderSettingsResponse(
    boolean automaticRemindersEnabled, List<PaymentReminderStep> steps) {}
