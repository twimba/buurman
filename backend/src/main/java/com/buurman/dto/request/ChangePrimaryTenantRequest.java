package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

public record ChangePrimaryTenantRequest(
    Optional<String> tenantIdentifier, @Valid Optional<CreateTenantRequest> newTenant) {

  public ChangePrimaryTenantRequest {
    tenantIdentifier = Objects.requireNonNullElse(tenantIdentifier, Optional.empty());
    newTenant = Objects.requireNonNullElse(newTenant, Optional.empty());
  }

  @AssertTrue(message = "Provide either tenantIdentifier or newTenant, not both") public boolean isValidPartySource() {
    boolean hasIdentifier = tenantIdentifier.isPresent() && !tenantIdentifier.get().isBlank();
    boolean hasNewTenant = newTenant.isPresent();
    return hasIdentifier ^ hasNewTenant;
  }
}
