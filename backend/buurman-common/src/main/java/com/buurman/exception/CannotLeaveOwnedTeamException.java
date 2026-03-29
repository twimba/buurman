package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class CannotLeaveOwnedTeamException extends BusinessRuleException {
  public CannotLeaveOwnedTeamException(String message) {
    super(message);
  }
}
