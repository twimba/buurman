package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.fasterxml.jackson.annotation.JsonCreator;

public final class RegistrationInvitationIdentifier extends Sid {

  private RegistrationInvitationIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static RegistrationInvitationIdentifier of(String value) {
    return new RegistrationInvitationIdentifier(value);
  }
}
