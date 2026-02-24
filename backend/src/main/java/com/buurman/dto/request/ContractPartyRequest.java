package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.ContractPartyRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ContractPartyRequest(
    Optional<String> tenantIdentifier,
    @Valid Optional<CreateTenantRequest> newTenant,
    @NotNull(message = "Role is required") ContractPartyRole role) {

  @AssertTrue(message = "Provide either tenantIdentifier or newTenant, not both") public boolean isValidPartySource() {
    boolean hasIdentifier = tenantIdentifier.isPresent() && !tenantIdentifier.get().isBlank();
    boolean hasNewTenant = newTenant.isPresent();
    return hasIdentifier ^ hasNewTenant;
  }
}
