package com.buurman.domain.identifier;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;
import com.fasterxml.jackson.annotation.JsonCreator;

@SkipTestCoverage
public final class LeaseClauseTemplateIdentifier extends Sid {

  private LeaseClauseTemplateIdentifier(String value) {
    super(value);
  }

  @JsonCreator
  public static LeaseClauseTemplateIdentifier of(String value) {
    return new LeaseClauseTemplateIdentifier(value);
  }
}
