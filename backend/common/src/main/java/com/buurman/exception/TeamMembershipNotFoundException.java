package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class TeamMembershipNotFoundException extends NotFoundException {
  public TeamMembershipNotFoundException(String message) {
    super(message);
  }
}
