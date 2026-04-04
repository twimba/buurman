package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class DocumentRenderException extends BuurmanException {
  public DocumentRenderException(String message) {
    super(message);
  }

  public DocumentRenderException(String message, Throwable cause) {
    super(message, cause);
  }
}
