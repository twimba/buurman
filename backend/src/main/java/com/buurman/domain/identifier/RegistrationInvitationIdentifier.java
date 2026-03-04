package com.buurman.domain.identifier;

import com.buurman.domain.Ulid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class RegistrationInvitationIdentifier extends Ulid {

  private RegistrationInvitationIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static RegistrationInvitationIdentifier of(String value) {
    return new RegistrationInvitationIdentifier(value);
  }
}
