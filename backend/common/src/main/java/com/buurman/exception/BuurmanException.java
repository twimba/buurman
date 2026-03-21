package com.buurman.exception;

import com.buurman.util.Generated;

@Generated
public abstract class BuurmanException extends RuntimeException {
  protected BuurmanException(String message) {
    super(message);
  }

  protected BuurmanException(String message, Throwable cause) {
    super(message, cause);
  }
}
