package com.buurman.dto.request;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@SkipTestCoverage
public record BulkCreateRentRegulationRulesRequest(
    @NotEmpty @Valid List<CreateRentRegulationRuleRequest> rules) {}
