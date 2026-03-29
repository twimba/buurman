package com.buurman.dto.request.backoffice;

import org.jspecify.annotations.Nullable;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record UpdateFeatureFlagRequest(@Nullable Boolean enabled, @Nullable String value) {}
