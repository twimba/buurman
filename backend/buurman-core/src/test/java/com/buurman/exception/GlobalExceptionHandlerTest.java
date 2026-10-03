package com.buurman.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

  private final MessageSource messageSource = mock(MessageSource.class);
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);
  private final MockHttpServletRequest request =
      new MockHttpServletRequest("POST", "/contracts/CNT1/lease-agreement");

  GlobalExceptionHandlerTest() {
    when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Conflict");
  }

  @Test
  void leaseNotAvailableIs409WithCodeProperty() {
    ProblemDetail problem =
        handler.handleLeaseNotAvailable(LeaseNotAvailableException.forCountry("IT"), request);

    assertThat(problem.getStatus()).isEqualTo(409);
    assertThat(problem.getTitle()).isEqualTo("Conflict");
    assertThat(problem.getDetail()).contains("IT");
    assertThat(problem.getInstance()).hasToString("/contracts/CNT1/lease-agreement");
    assertThat(problem.getProperties()).containsEntry("code", "LEASE_NOT_AVAILABLE_FOR_COUNTRY");
  }

  @Test
  void noCountryUsesItsOwnCode() {
    ProblemDetail problem =
        handler.handleLeaseNotAvailable(LeaseNotAvailableException.noCountry(), request);

    assertThat(problem.getProperties()).containsEntry("code", "LEASE_CONTRACT_HAS_NO_COUNTRY");
  }

  @Test
  void plainBusinessRuleExceptionHasNoCode() {
    ProblemDetail problem = handler.handleBusinessRule(new BusinessRuleException("nope"), request);

    assertThat(problem.getStatus()).isEqualTo(409);
    assertThat(problem.getProperties()).isNull();
  }
}
