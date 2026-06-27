package com.buurman.dto.request.backoffice.dashboard;

import com.buurman.util.SkipTestCoverage;

/** Snooze an action-queue item for {@code hours} hours. */
@SkipTestCoverage
public record SnoozeActionItemRequest(String itemKey, int hours) {}
