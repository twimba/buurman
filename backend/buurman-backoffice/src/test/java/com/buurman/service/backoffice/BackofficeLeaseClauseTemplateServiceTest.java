package com.buurman.service.backoffice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSource;

import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.LeaseClauseTemplateIdentifier;
import com.buurman.dto.request.backoffice.UpsertLeaseClauseTemplateRequest;
import com.buurman.dto.response.LeaseClauseTemplateResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.LeaseClauseTemplateRepository;
import com.buurman.service.letters.LeaseMessageCatalog;
import com.buurman.util.DocumentLanguages;

class BackofficeLeaseClauseTemplateServiceTest {

  private static final MessageSource MESSAGES =
      DocumentTemplateSupport.messageSource(true, "classpath:messages/document-lease-agreement");

  private static final LeaseMessageCatalog CATALOG = new LeaseMessageCatalog();

  private final LeaseClauseTemplateRepository repository =
      mock(LeaseClauseTemplateRepository.class);

  private final BackofficeLeaseClauseTemplateService service =
      new BackofficeLeaseClauseTemplateService(repository, MESSAGES, CATALOG);

  private static final UUID ACTOR_ID = UUID.randomUUID();

  private static UpsertLeaseClauseTemplateRequest request(
      String titleKey,
      boolean defaultIncluded,
      boolean optional,
      @Nullable LeaseKind leaseKind,
      boolean pinned) {
    return new UpsertLeaseClauseTemplateRequest(
        "NL",
        leaseKind,
        "governing-law",
        titleKey,
        "clause.governingLaw.body",
        defaultIncluded,
        optional,
        pinned,
        10);
  }

  private static LeaseClauseTemplate storedTemplate(LeaseKind kind) {
    return LeaseClauseTemplate.builder()
        .identifier(Optional.of(LeaseClauseTemplateIdentifier.of("LCT01")))
        .countryCode("NL")
        .leaseKind(kind)
        .clauseKey("governing-law")
        .titleI18nKey("clause.governingLaw.title")
        .bodyI18nKey("clause.governingLaw.body")
        .defaultIncluded(true)
        .optional(false)
        .sortOrder(10)
        .version(1)
        .build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"../x", "a b", "A", "a::b", "", "a_b"})
  @DisplayName("create rejects clause keys outside ^[a-z0-9-]{1,64}$")
  void createRejectsUnsafeClauseKey(String key) {
    UpsertLeaseClauseTemplateRequest bad =
        new UpsertLeaseClauseTemplateRequest(
            "NL", LeaseKind.RESIDENTIAL, key, "t", "b", true, false, false, 10);
    assertThatThrownBy(() -> service.create(bad, ACTOR_ID)).isInstanceOf(BadRequestException.class);
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("create accepts a seeded-style key")
  void createAcceptsTerminationReference() {
    when(repository.save(any(LeaseClauseTemplate.class)))
        .thenAnswer(
            i -> {
              LeaseClauseTemplate t = i.getArgument(0);
              t.setIdentifier(Optional.of(LeaseClauseTemplateIdentifier.of("LCT01")));
              return t;
            });
    assertThat(
            service
                .create(
                    new UpsertLeaseClauseTemplateRequest(
                        "NL",
                        LeaseKind.RESIDENTIAL,
                        "termination-reference",
                        "t",
                        "b",
                        true,
                        false,
                        false,
                        10),
                    ACTOR_ID)
                .clauseKey())
        .isEqualTo("termination-reference");
  }

  @Test
  @DisplayName("create persists and returns the right response shape")
  void createPersistsAndReturnsResponse() {
    UpsertLeaseClauseTemplateRequest request =
        request("clause.governingLaw.title", true, false, LeaseKind.RESIDENTIAL, false);

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
    assertThat(response.clauseKey()).isEqualTo("governing-law");
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
        request("clause.governingLaw.title", true, false, LeaseKind.RESIDENTIAL, false);
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
            .clauseKey("governing-law")
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
        request("clause.governingLaw.title (edited)", true, false, null, false);

    service.update(identifier, request, ACTOR_ID);

    org.mockito.ArgumentCaptor<LeaseClauseTemplate> captor =
        org.mockito.ArgumentCaptor.forClass(LeaseClauseTemplate.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getCreatedBy())
        .as("createdBy must not change on update")
        .isEqualTo(originalCreator);
    assertThat(captor.getValue().getUpdatedBy()).isEqualTo(ACTOR_ID);
    assertThat(captor.getValue().getVersion())
        .as("version must be bumped so a cached resolution keyed by it is detectably stale")
        .isEqualTo(2);
  }

  @Test
  @DisplayName("create rejects a required clause that defaults to excluded")
  void createRejectsRequiredButExcludedByDefault() {
    UpsertLeaseClauseTemplateRequest request =
        request("clause.governingLaw.title", false, false, LeaseKind.RESIDENTIAL, false);

    assertThatThrownBy(() -> service.create(request, ACTOR_ID))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("update rejects a required clause that defaults to excluded")
  void updateRejectsRequiredButExcludedByDefault() {
    Sid identifier = LeaseClauseTemplateIdentifier.of("LCT01");
    UpsertLeaseClauseTemplateRequest request =
        request("clause.governingLaw.title", false, false, null, false);

    assertThatThrownBy(() -> service.update(identifier, request, ACTOR_ID))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("update on a nonexistent identifier throws NotFoundException")
  void updateOnNonexistentIdentifierThrows() {
    Sid identifier = LeaseClauseTemplateIdentifier.of("LCT99");
    UpsertLeaseClauseTemplateRequest request =
        request("clause.governingLaw.title", true, false, null, false);

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
            .clauseKey("governing-law")
            .titleI18nKey("clause.governingLaw.title")
            .bodyI18nKey("clause.governingLaw.body")
            .defaultIncluded(true)
            .optional(false)
            .sortOrder(10)
            .version(1)
            .build();

    when(repository.findByCountryCode("NL")).thenReturn(List.of(nlTemplate));

    List<LeaseClauseTemplateResponse> responses =
        service.list("NL", Optional.empty(), Optional.of("en"));

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).countryCode()).isEqualTo("NL");
    verify(repository).findByCountryCode("NL");
  }

  @Test
  @DisplayName("create with an explicit kind and pinned persists both")
  void createPersistsKindAndPinned() {
    when(repository.save(any(LeaseClauseTemplate.class)))
        .thenAnswer(
            invocation -> {
              LeaseClauseTemplate template = invocation.getArgument(0);
              template.setIdentifier(Optional.of(LeaseClauseTemplateIdentifier.of("LCT01")));
              return template;
            });

    LeaseClauseTemplateResponse response =
        service.create(request("t", true, false, LeaseKind.COMMERCIAL, true), ACTOR_ID);

    org.mockito.ArgumentCaptor<LeaseClauseTemplate> captor =
        org.mockito.ArgumentCaptor.forClass(LeaseClauseTemplate.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getLeaseKind()).isEqualTo(LeaseKind.COMMERCIAL);
    assertThat(captor.getValue().isPinned()).isTrue();
    assertThat(response.leaseKind()).isEqualTo(LeaseKind.COMMERCIAL);
    assertThat(response.pinned()).isTrue();
  }

  @Test
  @DisplayName("create without a kind is rejected, never defaulted")
  void createRequiresKind() {
    UpsertLeaseClauseTemplateRequest noKind = request("t", true, false, null, false);

    assertThatThrownBy(() -> service.create(noKind, ACTOR_ID))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("leaseKind is required");
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("update rejects a kind different from the stored one")
  void updateRejectsKindChange() {
    Sid identifier = LeaseClauseTemplateIdentifier.of("LCT01");
    when(repository.getByIdentifier(identifier)).thenReturn(storedTemplate(LeaseKind.RESIDENTIAL));

    assertThatThrownBy(
            () ->
                service.update(
                    identifier, request("t", true, false, LeaseKind.COMMERCIAL, false), ACTOR_ID))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Lease kind of an existing template cannot be changed");
  }

  @Test
  @DisplayName("update accepts the same or omitted kind and may change pinned")
  void updateMayChangePinned() {
    Sid identifier = LeaseClauseTemplateIdentifier.of("LCT01");
    when(repository.getByIdentifier(identifier)).thenReturn(storedTemplate(LeaseKind.COMMERCIAL));
    when(repository.save(any(LeaseClauseTemplate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LeaseClauseTemplateResponse same =
        service.update(identifier, request("t", true, false, LeaseKind.COMMERCIAL, true), ACTOR_ID);
    LeaseClauseTemplateResponse omitted =
        service.update(identifier, request("t", true, false, null, false), ACTOR_ID);

    assertThat(same.pinned()).isTrue();
    assertThat(same.leaseKind()).isEqualTo(LeaseKind.COMMERCIAL);
    assertThat(omitted.pinned()).isFalse();
    assertThat(omitted.leaseKind()).isEqualTo(LeaseKind.COMMERCIAL);
  }

  @Test
  @DisplayName("list with a kind filters via findByCountryAndKind")
  void listWithKindFilters() {
    when(repository.findByCountryAndKind("NL", LeaseKind.COMMERCIAL))
        .thenReturn(List.of(storedTemplate(LeaseKind.COMMERCIAL)));

    List<LeaseClauseTemplateResponse> responses =
        service.list("NL", Optional.of(LeaseKind.COMMERCIAL), Optional.of("en"));

    assertThat(responses).hasSize(1);
    verify(repository).findByCountryAndKind("NL", LeaseKind.COMMERCIAL);
    verify(repository, org.mockito.Mockito.never()).findByCountryCode(any());
  }

  private static LeaseClauseTemplate nlDepositTemplate(String titleKey, String bodyKey) {
    LeaseClauseTemplate template = storedTemplate(LeaseKind.RESIDENTIAL);
    template.setTitleI18nKey(titleKey);
    template.setBodyI18nKey(bodyKey);
    return template;
  }

  @Test
  @DisplayName("list resolves title and body in the requested language")
  void listResolvesTextPerLanguage() {
    when(repository.findByCountryCode("NL"))
        .thenReturn(
            List.of(
                nlDepositTemplate(
                    "lease.nl.residential.deposit.title", "lease.nl.residential.deposit.summary")));

    LeaseClauseTemplateResponse en = service.list("NL", Optional.empty(), Optional.of("en")).get(0);
    LeaseClauseTemplateResponse nl = service.list("NL", Optional.empty(), Optional.of("nl")).get(0);
    LeaseClauseTemplateResponse de = service.list("NL", Optional.empty(), Optional.of("de")).get(0);

    assertThat(en.titleText()).isEqualTo("Deposit");
    assertThat(nl.titleText()).isEqualTo("Waarborgsom");
    assertThat(de.titleText()).isNotEqualTo("Deposit").isNotEqualTo(en.titleI18nKey());
    assertThat(nl.bodyText()).startsWith("Waarborgsom van ten hoogste");
    assertThat(nl.titleI18nKey()).isEqualTo("lease.nl.residential.deposit.title");
    assertThat(nl.missingLanguages()).isEmpty();
  }

  @Test
  @DisplayName("a key inherited from the English base counts as missing in other languages")
  void baseInheritanceReportedMissing() {
    when(repository.findByCountryCode("NL"))
        .thenReturn(
            List.of(
                nlDepositTemplate(
                    "lease.nl.residential.deposit.title", "lease.test.only.in.base")));
    LeaseMessageCatalog catalog = mock(LeaseMessageCatalog.class);
    when(catalog.missingLanguages("lease.nl.residential.deposit.title")).thenReturn(List.of());
    when(catalog.missingLanguages("lease.test.only.in.base")).thenReturn(List.of("nl", "de"));
    BackofficeLeaseClauseTemplateService mocked =
        new BackofficeLeaseClauseTemplateService(repository, MESSAGES, catalog);

    assertThat(mocked.list("NL", Optional.empty(), Optional.of("nl")).get(0).missingLanguages())
        .containsExactly("nl", "de");
  }

  @Test
  @DisplayName("missing languages merge title and body gaps in language order without duplicates")
  void missingLanguagesUnionOrdered() {
    when(repository.findByCountryCode("NL"))
        .thenReturn(List.of(nlDepositTemplate("t.key", "b.key")));
    LeaseMessageCatalog catalog = mock(LeaseMessageCatalog.class);
    when(catalog.missingLanguages("t.key")).thenReturn(List.of("nl", "sv"));
    when(catalog.missingLanguages("b.key")).thenReturn(List.of("nl", "de"));
    BackofficeLeaseClauseTemplateService mocked =
        new BackofficeLeaseClauseTemplateService(repository, MESSAGES, catalog);

    assertThat(mocked.list("NL", Optional.empty(), Optional.of("en")).get(0).missingLanguages())
        .containsExactly("nl", "de", "sv");
  }

  @Test
  @DisplayName("a key defined nowhere resolves to itself and is missing in all 13 languages")
  void keyMissingEverywhere() {
    when(repository.findByCountryCode("NL"))
        .thenReturn(List.of(nlDepositTemplate("lease.nope.title", "lease.nope.body")));

    LeaseClauseTemplateResponse response =
        service.list("NL", Optional.empty(), Optional.of("nl")).get(0);

    assertThat(response.titleText()).isEqualTo("lease.nope.title");
    assertThat(response.bodyText()).isEqualTo("lease.nope.body");
    assertThat(response.missingLanguages()).containsExactlyElementsOf(DocumentLanguages.ORDERED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"xx", "EN", "", "en-US"})
  @DisplayName("list rejects a language outside the 13-language allowlist")
  void listRejectsUnknownLanguage(String language) {
    assertThatThrownBy(() -> service.list("NL", Optional.empty(), Optional.of(language)))
        .isInstanceOf(BadRequestException.class);
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("list without a language resolves in English")
  void listDefaultsToEnglish() {
    when(repository.findByCountryCode("NL"))
        .thenReturn(
            List.of(
                nlDepositTemplate(
                    "lease.nl.residential.deposit.title", "lease.nl.residential.deposit.summary")));

    assertThat(service.list("NL", Optional.empty(), Optional.empty()).get(0).titleText())
        .isEqualTo("Deposit");
  }
}
