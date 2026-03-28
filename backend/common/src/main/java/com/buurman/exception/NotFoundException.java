package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class NotFoundException extends BuurmanException {
  public NotFoundException(String message) {
    super(message);
  }
}
