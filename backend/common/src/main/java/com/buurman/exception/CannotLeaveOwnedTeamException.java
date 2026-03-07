package com.buurman.exception;

public class CannotLeaveOwnedTeamException extends BusinessRuleException {
  public CannotLeaveOwnedTeamException(String message) {
    super(message);
  }
}
