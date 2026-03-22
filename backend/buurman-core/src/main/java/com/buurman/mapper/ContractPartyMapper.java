package com.buurman.mapper;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.ContractParty;
import com.buurman.dto.response.ContactSummary;
import com.buurman.dto.response.ContractPartyResponse;

@Component
public class ContractPartyMapper {

  public ContractPartyResponse toResponse(
      ContractParty party, @Nullable ContactSummary contactSummary) {
    return new ContractPartyResponse(
        party.getIdentifier().orElseThrow(), Optional.ofNullable(contactSummary), party.getRole());
  }
}
