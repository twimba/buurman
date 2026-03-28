package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record DuplicateMatch(ContactSummary contact, String matchField, String matchType) {}
