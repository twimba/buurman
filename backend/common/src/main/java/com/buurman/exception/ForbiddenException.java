package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class ForbiddenException extends BuurmanException {
  public ForbiddenException(String message) {
    super(message);
  }
}
