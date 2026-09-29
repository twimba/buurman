package com.buurman.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.dto.response.SignatureRequestResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.esignature.SignatureService;

class SignatureControllerTest {

  @Test
  void createSignatureRequestDelegatesToService() {
    SignatureService signatureService = mock(SignatureService.class);
    SignatureController controller = new SignatureController(signatureService);
    ContractIdentifier contractId = ContractIdentifier.of("CON00000000000000000000001");
    DocumentIdentifier documentId = DocumentIdentifier.of("DOC00000000000000000000001");
    SignatureRequestResponse expected =
        new SignatureRequestResponse(
            Sid.of("SGR00000000000000000000001"),
            Sid.of(documentId.value()),
            Optional.empty(),
            SignatureRequestStatus.PENDING,
            List.of(),
            Instant.now(),
            Instant.now());
    when(signatureService.createSignatureRequest(
            ArgumentMatchers.eq(contractId),
            ArgumentMatchers.eq(documentId),
            ArgumentMatchers.any()))
        .thenReturn(expected);

    // SecurityUtils.getCurrentPrincipal() reads from SecurityContextHolder; stub it directly
    // so this test exercises the delegation shape only.
    var authentication =
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
            null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    try {
      SignatureRequestResponse actual = controller.createSignatureRequest(contractId, documentId);
      assertThat(actual).isEqualTo(expected);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
