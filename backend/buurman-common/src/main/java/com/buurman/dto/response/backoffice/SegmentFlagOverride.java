package com.buurman.dto.response.backoffice;

import org.jspecify.annotations.Nullable;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record SegmentFlagOverride(
    @Nullable String flagName, boolean enabled, @Nullable Object value) {}
