package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.ContractPartyRole;

public record ContractPartyResponse(
    String identifier, Optional<TenantSummary> tenant, ContractPartyRole role) {}
