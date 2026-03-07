package com.buurman.exception;

public class EmailNotVerifiedException extends ForbiddenException {
  public EmailNotVerifiedException(String message) {
    super(message);
  }
}
