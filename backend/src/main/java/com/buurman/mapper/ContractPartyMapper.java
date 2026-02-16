package com.buurman.mapper;

import org.springframework.stereotype.Component;

import com.buurman.domain.ContractParty;
import com.buurman.dto.response.ContractPartyResponse;
import com.buurman.dto.response.TenantSummary;

@Component
public class ContractPartyMapper {

  public ContractPartyResponse toResponse(ContractParty party, TenantSummary tenantSummary) {
    return new ContractPartyResponse(party.getIdentifier(), tenantSummary, party.getRole());
  }
}
