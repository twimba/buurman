package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

/**
 * A rendered tenant reminder for one ladder step, shown in team settings. Built from a worked
 * example, never addressed to a real tenant and never sent.
 */
@SkipTestCoverage
public record ReminderPreviewResponse(
    String subject, String html, String languageTag, String sampleTenantName) {}
