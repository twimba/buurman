package com.buurman.dto.request;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Min;

@SkipTestCoverage
public record RentIncreasePreviewRequest(@Min(1900) int year) {}
