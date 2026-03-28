package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class CrossTeamAccessException extends ForbiddenException {
  public CrossTeamAccessException(String message) {
    super(message);
  }
}
