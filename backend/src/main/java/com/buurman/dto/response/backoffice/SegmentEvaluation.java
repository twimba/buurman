package com.buurman.dto.response.backoffice;

import java.util.Map;

import org.jspecify.annotations.Nullable;

public record SegmentEvaluation(
    long segmentId,
    @Nullable String segmentName,
    @Nullable String description,
    Map<String, SegmentFlagOverride> overrides) {}
