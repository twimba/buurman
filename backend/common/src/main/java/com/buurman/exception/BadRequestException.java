package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class BadRequestException extends BuurmanException {
  public BadRequestException(String message) {
    super(message);
  }
}
