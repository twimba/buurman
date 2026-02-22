package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.ContractPartyRole;

public record ContractPartyResponse(
    String identifier, @Nullable TenantSummary tenant, ContractPartyRole role) {}
