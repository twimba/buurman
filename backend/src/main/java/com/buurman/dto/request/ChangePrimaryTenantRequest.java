package com.buurman.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

public record ChangePrimaryTenantRequest(
    String tenantIdentifier, @Valid CreateTenantRequest newTenant) {

  @AssertTrue(message = "Provide either tenantIdentifier or newTenant, not both") public boolean isValidPartySource() {
    boolean hasIdentifier = tenantIdentifier != null && !tenantIdentifier.isBlank();
    boolean hasNewTenant = newTenant != null;
    return hasIdentifier ^ hasNewTenant;
  }
}
