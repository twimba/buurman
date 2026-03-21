package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buurman.domain.Contract.ContractStatus;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the {@code validateStatusTransition} private method of ContractService via reflection.
 * This is the core business rule for contract state transitions and is worth testing in isolation
 * without spinning up the full service (which has 13+ dependencies).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContractService — status transitions")
class ContractServiceTest {

  private Method validateStatusTransition;
  private Object serviceInstance;

  @BeforeEach
  void setUp() throws Exception {
    // Use reflection to access the private method since ContractService has 13+ constructor
    // dependencies that would require massive mock setup for no benefit.
    // We test the pure business logic only.
    Class<?> clazz = ContractService.class;
    validateStatusTransition =
        clazz.getDeclaredMethod(
            "validateStatusTransition", ContractStatus.class, ContractStatus.class);
    validateStatusTransition.setAccessible(true);

    // Create a mock with CALLS_REAL_METHODS so we can invoke the private method on it.
    // Since validateStatusTransition doesn't use any fields, this is safe.
    serviceInstance =
        org.mockito.Mockito.mock(
            ContractService.class,
            org.mockito.Mockito.withSettings()
                .defaultAnswer(org.mockito.Mockito.CALLS_REAL_METHODS));
  }

  private void invokeValidation(ContractStatus from, ContractStatus to) throws Exception {
    try {
      validateStatusTransition.invoke(serviceInstance, from, to);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw (Exception) e.getCause();
    }
  }

  @Nested
  @DisplayName("valid transitions")
  class ValidTransitions {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
      "DRAFT, PENDING_SIGNATURE",
      "DRAFT, ACTIVE",
      "PENDING_SIGNATURE, DRAFT",
      "PENDING_SIGNATURE, ACTIVE",
      "ACTIVE, TERMINATED",
      "ACTIVE, EXPIRED"
    })
    @DisplayName("accepts valid transition")
    void acceptsValidTransition(ContractStatus from, ContractStatus to) {
      assertThatCode(() -> invokeValidation(from, to)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("invalid transitions")
  class InvalidTransitions {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
      "DRAFT, TERMINATED",
      "DRAFT, EXPIRED",
      "PENDING_SIGNATURE, TERMINATED",
      "PENDING_SIGNATURE, EXPIRED",
      "ACTIVE, DRAFT",
      "ACTIVE, PENDING_SIGNATURE",
      "EXPIRED, DRAFT",
      "EXPIRED, ACTIVE",
      "EXPIRED, PENDING_SIGNATURE",
      "EXPIRED, TERMINATED",
      "TERMINATED, DRAFT",
      "TERMINATED, ACTIVE",
      "TERMINATED, PENDING_SIGNATURE",
      "TERMINATED, EXPIRED"
    })
    @DisplayName("rejects invalid transition")
    void rejectsInvalidTransition(ContractStatus from, ContractStatus to) {
      assertThatThrownBy(() -> invokeValidation(from, to))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid status transition");
    }
  }

  @Nested
  @DisplayName("terminal states")
  class TerminalStates {

    @Test
    @DisplayName("EXPIRED is a terminal state — no transitions out")
    void expiredIsTerminal() {
      for (ContractStatus to : ContractStatus.values()) {
        assertThatThrownBy(() -> invokeValidation(ContractStatus.EXPIRED, to))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Test
    @DisplayName("TERMINATED is a terminal state — no transitions out")
    void terminatedIsTerminal() {
      for (ContractStatus to : ContractStatus.values()) {
        assertThatThrownBy(() -> invokeValidation(ContractStatus.TERMINATED, to))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }
}
