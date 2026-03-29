package com.buurman.dto.response.backoffice;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record SegmentEvaluation(
    long segmentId,
    String segmentKey,
    @Nullable String segmentName,
    @Nullable String description,
    Map<String, SegmentFlagOverride> overrides) {}
