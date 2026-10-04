package com.buurman.service.esignature;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.SignatureRequestIdentifier;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.repository.SignatureSignerRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.S3StorageService;

@DisplayName("SignatureService method security for getSigningLinks")
class SignatureServiceAuthorizationTest {

  private static final SignatureRequestRepository REQUESTS = mock(SignatureRequestRepository.class);

  @Configuration
  @EnableMethodSecurity(prePostEnabled = true)
  static class Config {
    @Bean
    SignatureService signatureService() {
      return new SignatureService(
          mock(FeatureFlagService.class),
          mock(DocumentRepository.class),
          mock(ContractRepository.class),
          mock(ContractPartyRepository.class),
          mock(ContactRepository.class),
          mock(S3StorageService.class),
          mock(SignatureProviderClient.class),
          REQUESTS,
          mock(SignatureSignerRepository.class));
    }
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private static UserPrincipal as(String role) {
    UserPrincipal principal =
        new UserPrincipal(
            UUID.randomUUID(),
            "USR1",
            "kc-1",
            "u@example.com",
            "U",
            UUID.randomUUID(),
            "TEA1",
            null);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken(principal, null, List.of(() -> "ROLE_" + role)));
    return principal;
  }

  private static void call(UserPrincipal principal) {
    try (AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(Config.class)) {
      context
          .getBean(SignatureService.class)
          .getSigningLinks(
              ContractIdentifier.of("CON00000000000000000000001"),
              DocumentIdentifier.of("DOC00000000000000000000001"),
              SignatureRequestIdentifier.of("SGR00000000000000000000001"),
              principal);
    }
  }

  @Test
  @DisplayName("a TEAM_VIEWER is denied before any lookup happens")
  void viewerIsDenied() {
    org.mockito.Mockito.reset(REQUESTS);
    assertThatThrownBy(() -> call(as("TEAM_VIEWER"))).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(REQUESTS);
  }

  @ParameterizedTest
  @ValueSource(strings = {"TEAM_ADMIN", "TEAM_EDITOR"})
  @DisplayName("TEAM_ADMIN and TEAM_EDITOR pass through the method-security proxy")
  void adminAndEditorAreLetThrough(String role) {
    org.mockito.Mockito.reset(REQUESTS);
    when(REQUESTS.getByIdentifierAndTeamId(any(), any()))
        .thenThrow(new NotFoundException("Signature request not found"));
    assertThatThrownBy(() -> call(as(role))).isInstanceOf(NotFoundException.class);
  }
}
