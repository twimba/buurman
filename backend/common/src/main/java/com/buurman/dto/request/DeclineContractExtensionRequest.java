package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record DeclineContractExtensionRequest(Optional<String> reason) {}
