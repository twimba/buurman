package com.buurman.dto.response;

import com.buurman.domain.ContractPartyRole;

public record ContractPartyResponse(
    String identifier, TenantSummary tenant, ContractPartyRole role) {}
