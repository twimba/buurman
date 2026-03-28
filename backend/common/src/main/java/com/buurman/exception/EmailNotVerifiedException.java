package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class EmailNotVerifiedException extends ForbiddenException {
  public EmailNotVerifiedException(String message) {
    super(message);
  }
}
