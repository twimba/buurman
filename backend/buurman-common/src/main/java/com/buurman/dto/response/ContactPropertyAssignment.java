package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ContactPropertyAssignment(PropertySummary property, Optional<String> role) {}
