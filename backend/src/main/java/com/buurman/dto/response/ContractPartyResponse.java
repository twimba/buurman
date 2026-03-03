package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Ulid;

public record ContractPartyResponse(
    Ulid identifier, Optional<TenantSummary> tenant, ContractPartyRole role) {}
