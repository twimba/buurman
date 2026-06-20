package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

/** A single field that differs between the current database value and the bundled catalog value. */
@SkipTestCoverage
public record RentRegulationDiffField(String field, String before, String after) {}
