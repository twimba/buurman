package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contract;
import com.buurman.domain.Contract.ContractStatus;

/**
 * Uses reflection to test {@code validateContractEditable} because it is a pure business-rule
 * method (reads only {@code contract.getStatus()}) and {@code ContractPartyService} has 8
 * constructor dependencies — mocking them all provides no value for this check. Same technique as
 * {@code ContractServiceTest#validateStatusTransition}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContractPartyService — validateContractEditable")
class ContractPartyServiceTest {

  private Method validateContractEditable;
  private Object serviceInstance;

  @BeforeEach
  void setUp() throws Exception {
    validateContractEditable =
        ContractPartyService.class.getDeclaredMethod("validateContractEditable", Contract.class);
    validateContractEditable.setAccessible(true);

    // CALLS_REAL_METHODS so the private method body runs; it uses no fields, so this is safe.
    serviceInstance =
        Mockito.mock(
            ContractPartyService.class,
            Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
  }

  private void invokeValidation(ContractStatus status) throws Exception {
    Contract contract = Contract.builder().status(status).build();
    try {
      validateContractEditable.invoke(serviceInstance, contract);
    } catch (InvocationTargetException e) {
      throw (Exception) e.getCause();
    }
  }

  @Nested
  @DisplayName("blocked statuses")
  class BlockedStatuses {

    /**
     * NOTICE_GIVEN is locked like ACTIVE: once notice has been given, the contract's terms —
     * including who the parties are — back the termination record and notice letter already
     * generated, same reasoning as {@code ContractService#updateContract}/{@code deleteContract}.
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({"ACTIVE", "NOTICE_GIVEN", "TERMINATED", "EXPIRED"})
    @DisplayName("rejects modifying parties on a locked contract")
    void rejectsLockedContract(ContractStatus status) throws Exception {
      assertThatThrownBy(() -> invokeValidation(status))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
              switch (status) {
                case ACTIVE -> "Cannot modify parties on ACTIVE contracts.";
                case NOTICE_GIVEN ->
                    "Cannot modify parties on NOTICE_GIVEN contracts. Notice has already been"
                        + " given on this contract.";
                case TERMINATED -> "Cannot modify parties on TERMINATED contracts.";
                case EXPIRED -> "Cannot modify parties on EXPIRED contracts.";
                default -> throw new IllegalStateException("Unexpected status: " + status);
              });
    }
  }

  @Nested
  @DisplayName("editable statuses")
  class EditableStatuses {

    @ParameterizedTest(name = "{0}")
    @CsvSource({"DRAFT", "PENDING_SIGNATURE"})
    @DisplayName("allows modifying parties on a not-yet-in-force contract")
    void allowsEditableContract(ContractStatus status) {
      assertThatCode(() -> invokeValidation(status)).doesNotThrowAnyException();
    }
  }
}
