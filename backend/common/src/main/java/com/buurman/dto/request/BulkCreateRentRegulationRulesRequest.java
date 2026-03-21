package com.buurman.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import com.buurman.util.Generated;

@Generated
public record BulkCreateRentRegulationRulesRequest(
    @NotEmpty @Valid List<CreateRentRegulationRuleRequest> rules) {}
