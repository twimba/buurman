package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.ContractPartyRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record AddContractPartyRequest(
    Optional<String> tenantIdentifier,
    @Valid Optional<CreateTenantRequest> newTenant,
    @NotNull(message = "Role is required") ContractPartyRole role) {

  public AddContractPartyRequest {
    tenantIdentifier = Objects.requireNonNullElse(tenantIdentifier, Optional.empty());
    newTenant = Objects.requireNonNullElse(newTenant, Optional.empty());
  }

  @AssertTrue(message = "Provide either tenantIdentifier or newTenant, not both") public boolean isValidPartySource() {
    boolean hasIdentifier = tenantIdentifier.isPresent() && !tenantIdentifier.get().isBlank();
    boolean hasNewTenant = newTenant.isPresent();
    return hasIdentifier ^ hasNewTenant;
  }
}
