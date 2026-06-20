package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

/** Added / removed / changed tallies for one entity type (countries, regions or rules). */
@SkipTestCoverage
public record RentRegulationDiffCounts(int added, int removed, int changed) {}
