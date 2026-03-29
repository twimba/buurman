package com.buurman.dto.response;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record DuplicateCheckResponse(List<DuplicateMatch> matches) {}
