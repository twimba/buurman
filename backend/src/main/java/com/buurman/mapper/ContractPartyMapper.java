package com.buurman.mapper;

import com.buurman.domain.ContractParty;
import com.buurman.dto.response.ContractPartyResponse;
import com.buurman.dto.response.TenantSummary;
import org.springframework.stereotype.Component;

@Component
public class ContractPartyMapper {

    public ContractPartyResponse toResponse(ContractParty party, TenantSummary tenantSummary) {
        return new ContractPartyResponse(
                party.getIdentifier(),
                tenantSummary,
                party.getRole()
        );
    }
}
