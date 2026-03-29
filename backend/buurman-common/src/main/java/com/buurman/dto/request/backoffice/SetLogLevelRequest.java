package com.buurman.dto.request.backoffice;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record SetLogLevelRequest(Optional<String> level) {}
