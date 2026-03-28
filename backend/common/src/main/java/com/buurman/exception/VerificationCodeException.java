package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class VerificationCodeException extends BadRequestException {
  public VerificationCodeException(String message) {
    super(message);
  }
}
