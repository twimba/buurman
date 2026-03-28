package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public abstract class BuurmanException extends RuntimeException {
  protected BuurmanException(String message) {
    super(message);
  }

  protected BuurmanException(String message, Throwable cause) {
    super(message, cause);
  }
}
