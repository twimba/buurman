package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class BroadcastMessageIdentifier extends Sid {

  private BroadcastMessageIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static BroadcastMessageIdentifier of(String value) {
    return new BroadcastMessageIdentifier(value);
  }
}
