package com.buurman.mapper;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.ContractParty;
import com.buurman.dto.response.ContractPartyResponse;
import com.buurman.dto.response.TenantSummary;

@Component
public class ContractPartyMapper {

  public ContractPartyResponse toResponse(
      ContractParty party, @Nullable TenantSummary tenantSummary) {
    return new ContractPartyResponse(
        party.getIdentifier().orElseThrow(), Optional.ofNullable(tenantSummary), party.getRole());
  }
}
