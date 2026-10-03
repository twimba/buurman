package com.buurman.service.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.LeaseClauseTemplateIdentifier;
import com.buurman.dto.request.backoffice.UpsertLeaseClauseTemplateRequest;
import com.buurman.dto.response.LeaseClauseTemplateResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.LeaseClauseTemplateRepository;

class BackofficeLeaseClauseTemplateServiceTest {

  private final LeaseClauseTemplateRepository repository =
      mock(LeaseClauseTemplateRepository.class);

  private final BackofficeLeaseClauseTemplateService service =
      new BackofficeLeaseClauseTemplateService(repository);

  private static final UUID ACTOR_ID = UUID.randomUUID();

  @Test
  @DisplayName("create persists and returns the right response shape")
  void createPersistsAndReturnsResponse() {
    UpsertLeaseClauseTemplateRequest request =
        new UpsertLeaseClauseTemplateRequest(
            "NL",
            "governing_law",
            "clause.governingLaw.title",
            "clause.governingLaw.body",
            true,
            false,
            10);

    when(repository.save(any(LeaseClauseTemplate.class)))
        .thenAnswer(
            invocation -> {
              LeaseClauseTemplate template = invocation.getArgument(0);
              template.setIdentifier(Optional.of(LeaseClauseTemplateIdentifier.of("LCT01")));
              return template;
            });

    LeaseClauseTemplateResponse response = service.create(request, ACTOR_ID);

    assertThat(response.identifier().value()).isEqualTo("LCT01");
    assertThat(response.countryCode()).isEqualTo("NL");
    assertThat(response.clauseKey()).isEqualTo("governing_law");
    assertThat(response.titleI18nKey()).isEqualTo("clause.governingLaw.title");
    assertThat(response.bodyI18nKey()).isEqualTo("clause.governingLaw.body");
    assertThat(response.defaultIncluded()).isTrue();
    assertThat(response.optional()).isFalse();
    assertThat(response.sortOrder()).isEqualTo(10);
    assertThat(response.version()).isEqualTo(1);
  }

  @Test
  @DisplayName("create records the acting backoffice admin as both createdBy and updatedBy")
  void createRecordsActor() {
    UpsertLeaseClauseTemplateRequest request =
        new UpsertLeaseClauseTemplateRequest(
            "NL",
            "governing_law",
            "clause.governingLaw.title",
            "clause.governingLaw.body",
            true,
            false,
            10);
    when(repository.save(any(LeaseClauseTemplate.class)))
        .thenAnswer(
            invocation -> {
              LeaseClauseTemplate template = invocation.getArgument(0);
              template.setIdentifier(Optional.of(LeaseClauseTemplateIdentifier.of("LCT01")));
              return template;
            });

    service.create(request, ACTOR_ID);

    org.mockito.ArgumentCaptor<LeaseClauseTemplate> captor =
        org.mockito.ArgumentCaptor.forClass(LeaseClauseTemplate.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getCreatedBy()).isEqualTo(ACTOR_ID);
    assertThat(captor.getValue().getUpdatedBy()).isEqualTo(ACTOR_ID);
  }

  @Test
  @DisplayName(
      "update records the acting backoffice admin as updatedBy, without touching createdBy")
  void updateRecordsActorAsUpdatedByOnly() {
    UUID originalCreator = UUID.randomUUID();
    Sid identifier = LeaseClauseTemplateIdentifier.of("LCT01");
    LeaseClauseTemplate existing =
        LeaseClauseTemplate.builder()
            .identifier(Optional.of(identifier))
            .countryCode("NL")
            .clauseKey("governing_law")
            .titleI18nKey("clause.governingLaw.title")
            .bodyI18nKey("clause.governingLaw.body")
            .defaultIncluded(true)
            .optional(false)
            .sortOrder(10)
            .version(1)
            .createdBy(originalCreator)
            .updatedBy(originalCreator)
            .build();
    when(repository.getByIdentifier(identifier)).thenReturn(existing);
    when(repository.save(any(LeaseClauseTemplate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    UpsertLeaseClauseTemplateRequest request =
        new UpsertLeaseClauseTemplateRequest(
            "NL",
            "governing_law",
            "clause.governingLaw.title (edited)",
            "clause.governingLaw.body",
            true,
            false,
            10);

    service.update(identifier, request, ACTOR_ID);

    org.mockito.ArgumentCaptor<LeaseClauseTemplate> captor =
        org.mockito.ArgumentCaptor.forClass(LeaseClauseTemplate.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getCreatedBy())
        .as("createdBy must not change on update")
        .isEqualTo(originalCreator);
    assertThat(captor.getValue().getUpdatedBy()).isEqualTo(ACTOR_ID);
  }

  @Test
  @DisplayName("create rejects a required clause that defaults to excluded")
  void createRejectsRequiredButExcludedByDefault() {
    UpsertLeaseClauseTemplateRequest request =
        new UpsertLeaseClauseTemplateRequest(
            "NL",
            "governing_law",
            "clause.governingLaw.title",
            "clause.governingLaw.body",
            false,
            false,
            10);

    assertThatThrownBy(() -> service.create(request, ACTOR_ID))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("update rejects a required clause that defaults to excluded")
  void updateRejectsRequiredButExcludedByDefault() {
    Sid identifier = LeaseClauseTemplateIdentifier.of("LCT01");
    UpsertLeaseClauseTemplateRequest request =
        new UpsertLeaseClauseTemplateRequest(
            "NL",
            "governing_law",
            "clause.governingLaw.title",
            "clause.governingLaw.body",
            false,
            false,
            10);

    assertThatThrownBy(() -> service.update(identifier, request, ACTOR_ID))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("update on a nonexistent identifier throws NotFoundException")
  void updateOnNonexistentIdentifierThrows() {
    Sid identifier = LeaseClauseTemplateIdentifier.of("LCT99");
    UpsertLeaseClauseTemplateRequest request =
        new UpsertLeaseClauseTemplateRequest(
            "NL",
            "governing_law",
            "clause.governingLaw.title",
            "clause.governingLaw.body",
            true,
            false,
            10);

    when(repository.getByIdentifier(identifier))
        .thenThrow(new NotFoundException("Lease clause template not found"));

    assertThatThrownBy(() -> service.update(identifier, request, ACTOR_ID))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("list returns templates for the requested country only")
  void listReturnsTemplatesForRequestedCountryOnly() {
    LeaseClauseTemplate nlTemplate =
        LeaseClauseTemplate.builder()
            .identifier(Optional.of(LeaseClauseTemplateIdentifier.of("LCT01")))
            .countryCode("NL")
            .clauseKey("governing_law")
            .titleI18nKey("clause.governingLaw.title")
            .bodyI18nKey("clause.governingLaw.body")
            .defaultIncluded(true)
            .optional(false)
            .sortOrder(10)
            .version(1)
            .build();

    when(repository.findByCountryCode("NL")).thenReturn(List.of(nlTemplate));

    List<LeaseClauseTemplateResponse> responses = service.list("NL");

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).countryCode()).isEqualTo("NL");
    verify(repository).findByCountryCode("NL");
  }
}
