package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.ContractPartyRole;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ContractPartyRequest(
    Optional<String> contactIdentifier,
    @Valid Optional<CreateContactRequest> newContact,
    @NotNull(message = "Role is required") ContractPartyRole role) {

  @AssertTrue(message = "Provide either contactIdentifier or newContact, not both")
  public boolean isValidPartySource() {
    boolean hasIdentifier = contactIdentifier.isPresent() && !contactIdentifier.get().isBlank();
    boolean hasNewContact = newContact.isPresent();
    return hasIdentifier ^ hasNewContact;
  }
}
