package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.service.LeaseClauseResolver;

@DisplayName("LeasePreviewService method security")
class LeasePreviewAuthorizationTest {

  private static final LeaseAgreementExporter EXPORTER = mock(LeaseAgreementExporter.class);
  private static final LeaseClauseResolver RESOLVER = mock(LeaseClauseResolver.class);

  @Configuration
  @EnableMethodSecurity(prePostEnabled = true)
  static class Config {
    @Bean
    LeaseAgreementExporter realExporter() {
      return new LeaseAgreementExporter(
          mock(com.buurman.repository.ContractRepository.class),
          mock(com.buurman.repository.PropertyRepository.class),
          mock(com.buurman.repository.ContractRentComponentRepository.class),
          mock(com.buurman.repository.ContractRentPeriodRepository.class),
          RESOLVER,
          mock(com.buurman.service.LeaseKindResolver.class),
          mock(LeaseDocumentLocator.class),
          mock(com.buurman.repository.TeamRepository.class),
          mock(LetterExporterHelper.class),
          mock(LetterTemplateService.class),
          mock(MessageSource.class),
          Clock.system(ZoneOffset.UTC));
    }

    @Bean
    LeasePreviewService leasePreviewService() {
      return new LeasePreviewService(
          EXPORTER, RESOLVER, mock(MessageSource.class), Clock.system(ZoneOffset.UTC));
    }
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
    reset(EXPORTER, RESOLVER);
  }

  private static void callAs(String role) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new TestingAuthenticationToken("u", null, List.of(() -> "ROLE_" + role)));
    try (AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(Config.class)) {
      context.getBean(LeasePreviewService.class).preview(null);
    }
  }

  @ParameterizedTest(name = "{0} is denied")
  @ValueSource(strings = {"TEAM_ADMIN", "TEAM_EDITOR", "TEAM_VIEWER", "BACKOFFICE_VIEWER"})
  void nonBackofficeAdminIsDenied(String role) {
    assertThatThrownBy(() -> callAs(role)).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(EXPORTER, RESOLVER);
  }

  @Test
  @DisplayName("BACKOFFICE_ADMIN passes the gate (and then fails request validation)")
  void backofficeAdminPassesTheGate() {
    assertThatThrownBy(() -> callAs("BACKOFFICE_ADMIN"))
        .isNotInstanceOf(AccessDeniedException.class)
        .isInstanceOf(com.buurman.exception.BadRequestException.class);
  }

  @Test
  @DisplayName("package-private seams still reach the target through the security proxy")
  void packagePrivateSeamsWorkThroughProxy() {
    when(RESOLVER.templatesFor(Optional.of("NL"), LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(mock(LeaseClauseTemplate.class)));
    try (AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(Config.class)) {
      LeaseAgreementExporter proxied = context.getBean(LeaseAgreementExporter.class);
      assertThat(AopUtils.isAopProxy(proxied)).isTrue();

      assertThat(proxied.plan(Optional.of("NL"), LeaseKind.RESIDENTIAL, "en").country())
          .contains("NL");
    }
  }
}
