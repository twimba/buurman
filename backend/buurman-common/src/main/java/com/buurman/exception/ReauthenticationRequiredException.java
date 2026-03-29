package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class ReauthenticationRequiredException extends BuurmanException {
  public ReauthenticationRequiredException(String message) {
    super(message);
  }
}
