package com.buurman;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.buurman.controller.SignatureController;
import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.dto.response.SignatureSigningLinkResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.ExternalServiceException;
import com.buurman.exception.GlobalExceptionHandler;
import com.buurman.security.UserPrincipal;
import com.buurman.service.esignature.SignatureService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Real Spring MVC dispatch (generated {@code SignaturesApi} mappings, real controller with a real
 * request-scoped {@link HttpServletResponse}, real {@link GlobalExceptionHandler}); only the
 * service is mocked. It proves the controller itself sends {@code Cache-Control: no-store} and that
 * 403/409/502 are rendered by the real exception handler. It does NOT include the Spring Security
 * filter chain, so the header asserted here comes from the controller, not from Spring Security's
 * defaults; method-level 403 enforcement is covered by SignatureServiceAuthorizationTest.
 */
@DisplayName("GET signing-links over the real MVC stack")
class SignatureSigningLinksHttpTest {

  private static final String URL =
      "/contracts/CON00000000000000000000001/documents/DOC00000000000000000000001"
          + "/signature-requests/SGR00000000000000000000001/signing-links";

  private static final SignatureService SERVICE = mock(SignatureService.class);

  @Configuration
  @EnableWebMvc
  static class Config {
    @Bean
    SignatureService signatureService() {
      return SERVICE;
    }

    @Bean
    SignatureController signatureController(
        SignatureService signatureService, HttpServletResponse response) {
      return new SignatureController(signatureService, response);
    }

    @Bean
    GlobalExceptionHandler globalExceptionHandler() {
      return new GlobalExceptionHandler(mock(MessageSource.class));
    }
  }

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.reset(SERVICE);
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(Config.class);
    context.refresh();
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
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
  @DisplayName("200 carries the links and Cache-Control: no-store")
  void successIsNotCacheable() throws Exception {
    when(SERVICE.getSigningLinks(any(), any(), any(), any()))
        .thenReturn(
            List.of(
                new SignatureSigningLinkResponse(
                    "Lena",
                    "l@example.com",
                    SignatureSignerRole.LANDLORD,
                    SignatureSignerStatus.PENDING,
                    Optional.of("https://sign.example.com/sign/tok"),
                    false)));

    mockMvc
        .perform(get(URL))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().string("Pragma", "no-cache"))
        .andExpect(jsonPath("$[0].signingUrl").value("https://sign.example.com/sign/tok"))
        .andExpect(jsonPath("$[0].signed").value(false));
  }

  @Test
  @DisplayName("a forbidden caller gets 403 from the real exception handler")
  void forbiddenIs403() throws Exception {
    when(SERVICE.getSigningLinks(any(), any(), any(), any()))
        .thenThrow(new AccessDeniedException("denied"));

    mockMvc.perform(get(URL)).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("a final-state request gets 409")
  void finalStateIs409() throws Exception {
    when(SERVICE.getSigningLinks(any(), any(), any(), any()))
        .thenThrow(new BusinessRuleException("This signature request is completed"));

    mockMvc.perform(get(URL)).andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Documenso being down gets 502")
  void providerDownIs502() throws Exception {
    when(SERVICE.getSigningLinks(any(), any(), any(), any()))
        .thenThrow(new ExternalServiceException("We couldn't load the signing links."));

    mockMvc.perform(get(URL)).andExpect(status().isBadGateway());
  }
}
