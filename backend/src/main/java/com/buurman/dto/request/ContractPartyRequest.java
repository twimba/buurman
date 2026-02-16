package com.buurman.dto.request;

import com.buurman.domain.ContractPartyRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ContractPartyRequest(
    String tenantIdentifier,
    @Valid CreateTenantRequest newTenant,
    @NotNull(message = "Role is required") ContractPartyRole role) {
  @AssertTrue(message = "Provide either tenantIdentifier or newTenant, not both") public boolean isValidPartySource() {
    boolean hasIdentifier = tenantIdentifier != null && !tenantIdentifier.isBlank();
    boolean hasNewTenant = newTenant != null;
    return hasIdentifier ^ hasNewTenant;
  }
}
