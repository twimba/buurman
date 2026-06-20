package com.buurman.dto.response;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

/**
 * The set of changes a catalog reload would apply within a single country. Only countries with at
 * least one change appear in the diff.
 */
@SkipTestCoverage
public record RentRegulationCountryDiff(
    String countryCode,
    String countryName,
    String status, // ADDED | REMOVED | MODIFIED
    RentRegulationDiffCounts rules,
    List<RentRegulationDiffEntry> changes) {}
