package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Sid;

public record ContractPartyResponse(
    Sid identifier, Optional<TenantSummary> tenant, ContractPartyRole role) {}
