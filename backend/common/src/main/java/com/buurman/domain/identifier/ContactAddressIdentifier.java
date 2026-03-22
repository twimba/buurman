package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;

@Generated
public final class ContactAddressIdentifier extends Sid {

  private ContactAddressIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static ContactAddressIdentifier of(String value) {
    return new ContactAddressIdentifier(value);
  }
}
