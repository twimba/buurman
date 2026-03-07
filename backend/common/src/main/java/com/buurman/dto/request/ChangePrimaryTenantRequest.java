package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

public record ChangePrimaryTenantRequest(
    Optional<String> tenantIdentifier, @Valid Optional<CreateTenantRequest> newTenant) {

  @AssertTrue(message = "Provide either tenantIdentifier or newTenant, not both") public boolean isValidPartySource() {
    boolean hasIdentifier = tenantIdentifier.isPresent() && !tenantIdentifier.get().isBlank();
    boolean hasNewTenant = newTenant.isPresent();
    return hasIdentifier ^ hasNewTenant;
  }
}
