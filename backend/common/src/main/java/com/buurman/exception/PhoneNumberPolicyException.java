package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class PhoneNumberPolicyException extends BadRequestException {
  public PhoneNumberPolicyException(String message) {
    super(message);
  }
}
