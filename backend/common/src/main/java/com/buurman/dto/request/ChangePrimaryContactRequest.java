package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

public record ChangePrimaryContactRequest(
    Optional<String> contactIdentifier, @Valid Optional<CreateContactRequest> newContact) {

  @AssertTrue(message = "Provide either contactIdentifier or newContact, not both")
  public boolean isValidPartySource() {
    boolean hasIdentifier = contactIdentifier.isPresent() && !contactIdentifier.get().isBlank();
    boolean hasNewContact = newContact.isPresent();
    return hasIdentifier ^ hasNewContact;
  }
}
