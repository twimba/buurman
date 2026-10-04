package com.buurman.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.security.UserPrincipal;
import com.buurman.service.LeaseClauseService;
import com.buurman.service.letters.LeaseAgreementGenerationService;

class LeaseAgreementControllerTest {

  private static final ContractIdentifier CONTRACT =
      ContractIdentifier.of("CON00000000000000000000001");

  private final LeaseAgreementGenerationService generationService =
      mock(LeaseAgreementGenerationService.class);
  private final LeaseAgreementController controller =
      new LeaseAgreementController(mock(LeaseClauseService.class), generationService);
  private final DocumentResponse expected =
      new DocumentResponse(
          Sid.of("DOC00000000000000000000001"),
          "CONTRACT",
          Sid.of(CONTRACT.value()),
          "key",
          "lease-agreement-CON00000000000000000000001-nl.pdf",
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Instant.now(),
          Optional.empty());

  @BeforeEach
  void authenticate() {
    // SecurityUtils.getCurrentPrincipal() reads from SecurityContextHolder.
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(
                new UserPrincipal(
                    UUID.randomUUID(),
                    "USR1",
                    "kc-1",
                    "l@example.com",
                    "L",
                    UUID.randomUUID(),
                    "TEA1",
                    null),
                null));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void passesTheRequestedLanguageThrough() {
    when(generationService.generateAndPersist(eq(CONTRACT), eq(Optional.of("nl")), any()))
        .thenReturn(expected);

    assertThat(controller.generateLeaseAgreement(CONTRACT, Optional.of("nl"))).isEqualTo(expected);
  }

  @Test
  void absentLanguageLeavesTheChoiceToTheService() {
    when(generationService.generateAndPersist(eq(CONTRACT), eq(Optional.empty()), any()))
        .thenReturn(expected);

    assertThat(controller.generateLeaseAgreement(CONTRACT, Optional.empty())).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"xx", "ja", "EN-GB", ""})
  void rejectsAnUnsupportedLanguageWithoutGenerating(String lang) {
    assertThatThrownBy(() -> controller.generateLeaseAgreement(CONTRACT, Optional.of(lang)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unsupported document language");
    verifyNoInteractions(generationService);
  }
}
