package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record InfoResponse(String version, String environment) {}
