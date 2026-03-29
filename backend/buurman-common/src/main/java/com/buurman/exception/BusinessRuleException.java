package com.buurman.exception;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public class BusinessRuleException extends BuurmanException {
  public BusinessRuleException(String message) {
    super(message);
  }
}
