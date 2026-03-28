package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class TeamNotFoundException extends NotFoundException {
  public TeamNotFoundException(String message) {
    super(message);
  }
}
