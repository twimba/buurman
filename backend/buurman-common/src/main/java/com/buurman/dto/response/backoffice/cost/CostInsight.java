package com.buurman.dto.response.backoffice.cost;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

/** A derived headline figure (e.g. cost-to-serve per team). */
@SkipTestCoverage
public record CostInsight(String label, String value, Optional<String> hint) {}
