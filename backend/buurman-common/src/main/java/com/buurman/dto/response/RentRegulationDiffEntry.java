package com.buurman.dto.response;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

/**
 * A single change a catalog reload would apply: an added, removed or changed country / region /
 * rule. {@code fields} lists the differing values for a CHANGED entry (empty for ADDED/REMOVED).
 */
@SkipTestCoverage
public record RentRegulationDiffEntry(
    String entity, // COUNTRY | REGION | RULE
    String op, // ADDED | REMOVED | CHANGED
    String label, // human-readable identity, e.g. "2026 · REGULATED · national"
    List<RentRegulationDiffField> fields) {}
