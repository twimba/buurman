package com.buurman.exception;

public class ReauthenticationRequiredException extends BuurmanException {
  public ReauthenticationRequiredException(String message) {
    super(message);
  }
}
