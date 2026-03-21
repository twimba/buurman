package com.buurman.dto.request;

import java.util.List;

import com.buurman.util.Generated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Generated
public record BulkCreateRentRegulationRulesRequest(
    @NotEmpty @Valid List<CreateRentRegulationRuleRequest> rules) {}
