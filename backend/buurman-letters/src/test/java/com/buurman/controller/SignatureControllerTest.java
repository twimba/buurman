package com.buurman.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.SignatureRequestIdentifier;
import com.buurman.dto.response.SignatureRequestResponse;
import com.buurman.dto.response.SignatureSigningLinkResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.esignature.SignatureService;

import jakarta.servlet.http.HttpServletResponse;

class SignatureControllerTest {

  @Test
  void createSignatureRequestDelegatesToService() {
    SignatureService signatureService = mock(SignatureService.class);
    SignatureController controller =
        new SignatureController(signatureService, mock(HttpServletResponse.class));
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

  @Test
  void listSignatureRequestsDelegatesToService() {
    SignatureService signatureService = mock(SignatureService.class);
    SignatureController controller =
        new SignatureController(signatureService, mock(HttpServletResponse.class));
    ContractIdentifier contractId = ContractIdentifier.of("CON00000000000000000000001");
    DocumentIdentifier documentId = DocumentIdentifier.of("DOC00000000000000000000001");
    SignatureRequestResponse newest =
        new SignatureRequestResponse(
            Sid.of("SGR00000000000000000000002"),
            Sid.of(documentId.value()),
            Optional.empty(),
            SignatureRequestStatus.PENDING,
            List.of(),
            Instant.now(),
            Instant.now());
    SignatureRequestResponse older =
        new SignatureRequestResponse(
            Sid.of("SGR00000000000000000000001"),
            Sid.of(documentId.value()),
            Optional.empty(),
            SignatureRequestStatus.DECLINED,
            List.of(),
            Instant.now().minusSeconds(3600),
            Instant.now().minusSeconds(3600));
    when(signatureService.listSignatureRequests(
            ArgumentMatchers.eq(contractId),
            ArgumentMatchers.eq(documentId),
            ArgumentMatchers.any()))
        .thenReturn(List.of(newest, older));

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
      assertThat(controller.listSignatureRequests(contractId, documentId))
          .containsExactly(newest, older);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void getSigningLinksDelegatesAndForbidsCaching() {
    SignatureService signatureService = mock(SignatureService.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    SignatureController controller = new SignatureController(signatureService, response);
    ContractIdentifier contractId = ContractIdentifier.of("CON00000000000000000000001");
    DocumentIdentifier documentId = DocumentIdentifier.of("DOC00000000000000000000001");
    SignatureRequestIdentifier requestId =
        SignatureRequestIdentifier.of("SGR00000000000000000000001");
    var link =
        new SignatureSigningLinkResponse(
            "Lena",
            "l@example.com",
            SignatureSignerRole.LANDLORD,
            SignatureSignerStatus.PENDING,
            Optional.of("https://sign.example.com/sign/tok"),
            false);
    when(signatureService.getSigningLinks(
            ArgumentMatchers.eq(contractId),
            ArgumentMatchers.eq(documentId),
            ArgumentMatchers.eq(requestId),
            ArgumentMatchers.any()))
        .thenReturn(List.of(link));

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
    try {
      assertThat(controller.getSigningLinks(contractId, documentId, requestId))
          .containsExactly(link);
    } finally {
      SecurityContextHolder.clearContext();
    }
    verify(response).setHeader("Cache-Control", "no-store");
    verify(response).setHeader("Pragma", "no-cache");
  }
}
