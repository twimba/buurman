package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class InsufficientPermissionsException extends ForbiddenException {
  public InsufficientPermissionsException(String message) {
    super(message);
  }
}
