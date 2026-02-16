package com.buurman.exception;

public class ExternalServiceException extends BuurmanException {
  public ExternalServiceException(String message) {
    super(message);
  }

  public ExternalServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
