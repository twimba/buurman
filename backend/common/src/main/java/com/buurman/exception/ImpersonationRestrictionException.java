package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class ImpersonationRestrictionException extends BuurmanException {
  public ImpersonationRestrictionException(String message) {
    super(message);
  }
}
