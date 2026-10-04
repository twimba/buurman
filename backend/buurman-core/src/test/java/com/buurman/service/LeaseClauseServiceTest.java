package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.LeaseAvailability;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.UpdateContractLeaseClausesRequest;
import com.buurman.dto.request.UpdateContractLeaseClausesRequest.ClauseSelection;
import com.buurman.dto.response.LeaseClausesResponse;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.LeaseNotAvailableException;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.DocumentLanguages;

class LeaseClauseServiceTest {

  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final ContractLeaseClauseRepository overrideRepository =
      mock(ContractLeaseClauseRepository.class);
  private final LeaseClauseResolver resolver = mock(LeaseClauseResolver.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final LeaseKindResolver leaseKindResolver = mock(LeaseKindResolver.class);
  private final LeaseDocumentLanguageCatalog documentLanguageCatalog =
      mock(LeaseDocumentLanguageCatalog.class);

  private final LeaseClauseService service =
      new LeaseClauseService(
          contractRepository,
          overrideRepository,
          resolver,
          propertyRepository,
          leaseKindResolver,
          documentLanguageCatalog);

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final ContractIdentifier CONTRACT_IDENTIFIER =
      ContractIdentifier.of("CNT0000000000000000000000001");

  private final UserPrincipal principal =
      new UserPrincipal(
          USER_ID,
          "USR0000000000000000000000001",
          "keycloak-id",
          "landlord@example.com",
          "Landlord",
          TEAM_ID,
          "TEM0000000000000000000000001",
          TeamRole.TEAM_ADMIN);

  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final UUID UNIT_ID = UUID.randomUUID();

  @BeforeEach
  void stubKind() {
    Property property = Property.builder().id(PROPERTY_ID).build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);
    when(leaseKindResolver.resolveFor(any(), any(), eq(TEAM_ID))).thenReturn(LeaseKind.RESIDENTIAL);
  }

  private Contract contract() {
    return contractIn(Optional.of("NL"));
  }

  private Contract contractIn(Optional<String> country) {
    return Contract.builder()
        .id(CONTRACT_ID)
        .propertyId(PROPERTY_ID)
        .unitId(UNIT_ID)
        .teamId(TEAM_ID)
        .countryCode(country)
        .documentLanguages(List.of("en"))
        .build();
  }

  private LeaseClauseTemplate template(
      Sid identifier, String key, boolean optional, int sortOrder) {
    return LeaseClauseTemplate.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(identifier))
        .countryCode("NL")
        .clauseKey(key)
        .titleI18nKey("lease." + key + ".title")
        .bodyI18nKey("lease." + key + ".body")
        .defaultIncluded(true)
        .optional(optional)
        .sortOrder(sortOrder)
        .version(1)
        .build();
  }

  @Test
  void excludingNonOptionalClauseThrowsBeforeAnyWrite() {
    Sid templateIdentifier = Sid.of("LCT0000000000000000000000001");
    LeaseClauseTemplate mandatoryClause =
        template(templateIdentifier, "parties", /* optional= */ false, 1);

    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(available(mandatoryClause));

    UpdateContractLeaseClausesRequest request =
        new UpdateContractLeaseClausesRequest(
            List.of(new ClauseSelection(templateIdentifier.value(), false, 1)));

    assertThatThrownBy(() -> service.updateClauses(CONTRACT_IDENTIFIER, request, principal))
        .isInstanceOf(BadRequestException.class);

    // The review-focus assertion: the rejection must happen before any write, not merely before
    // the method returns.
    verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any());
  }

  @Test
  void duplicateTemplateIdentifierThrowsBeforeAnyWrite() {
    Sid templateIdentifier = Sid.of("LCT0000000000000000000000001");

    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());

    UpdateContractLeaseClausesRequest request =
        new UpdateContractLeaseClausesRequest(
            List.of(
                new ClauseSelection(templateIdentifier.value(), true, 1),
                new ClauseSelection(templateIdentifier.value(), false, 2)));

    assertThatThrownBy(() -> service.updateClauses(CONTRACT_IDENTIFIER, request, principal))
        .isInstanceOf(BadRequestException.class);

    // The review-focus assertion: the rejection must happen before any write, not merely before
    // the method returns — and before the template lookup that would otherwise follow.
    verify(resolver, never()).availabilityFor(any(), any());
    verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any());
  }

  @Test
  void unknownTemplateIdentifierThrowsBadRequest() {
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(available());

    UpdateContractLeaseClausesRequest request =
        new UpdateContractLeaseClausesRequest(
            List.of(new ClauseSelection("LCT0000000000000000000000099", true, 1)));

    assertThatThrownBy(() -> service.updateClauses(CONTRACT_IDENTIFIER, request, principal))
        .isInstanceOf(BadRequestException.class);

    verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any());
  }

  @Test
  void contractWithoutUnitResolvesKindThroughRealResolverWithoutUnitLookup() {
    var unitRepo = mock(UnitResidentialDetailsRepository.class);
    var realService =
        new LeaseClauseService(
            contractRepository,
            overrideRepository,
            resolver,
            propertyRepository,
            new LeaseKindResolver(unitRepo),
            documentLanguageCatalog);
    Contract noUnit =
        Contract.builder()
            .id(CONTRACT_ID)
            .teamId(TEAM_ID)
            .propertyId(PROPERTY_ID)
            .countryCode(Optional.of("NL"))
            .documentLanguages(List.of("en"))
            .build();
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(noUnit);
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(available());
    when(resolver.resolve(any(), any(), any(), anyList())).thenReturn(List.of());

    realService.getClauses(CONTRACT_IDENTIFIER, principal);

    verify(resolver).availabilityFor(any(), eq(LeaseKind.RESIDENTIAL));
    verifyNoInteractions(unitRepo);
  }

  @Test
  void getClausesResolvesWithDerivedKind() {
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(available());
    when(resolver.resolve(any(), any(), any(), anyList())).thenReturn(List.of());

    assertThat(service.getClauses(CONTRACT_IDENTIFIER, principal).clauses()).isEmpty();

    verify(resolver).availabilityFor(any(), eq(LeaseKind.RESIDENTIAL));
  }

  private static LeaseClauseResolver.Availability available(LeaseClauseTemplate... templates) {
    return new LeaseClauseResolver.Availability(
        LeaseAvailability.AVAILABLE_DOCUMENT, List.of(templates));
  }

  private ResolvedLeaseClauseResponse resolvedClause() {
    return new ResolvedLeaseClauseResponse(
        Sid.of("LCT0000000000000000000000001"),
        "parties",
        "Title",
        "Body",
        true,
        false,
        1,
        true,
        1);
  }

  @Test
  void getClausesReturnsEnvelopeWithResolvedClausesForAvailableState() {
    LeaseClauseTemplate t = template(Sid.of("LCT0000000000000000000000001"), "parties", false, 1);
    var availability =
        new LeaseClauseResolver.Availability(LeaseAvailability.AVAILABLE_EXAMPLE_TEXT, List.of(t));
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(availability);
    when(resolver.resolve(any(), any(), any(), eq(List.of(t))))
        .thenReturn(List.of(resolvedClause()));

    LeaseClausesResponse response = service.getClauses(CONTRACT_IDENTIFIER, principal);

    assertThat(response.availability()).isEqualTo(LeaseAvailability.AVAILABLE_EXAMPLE_TEXT);
    assertThat(response.countryCode()).contains("NL");
    assertThat(response.clauses()).containsExactly(resolvedClause());
  }

  @Test
  void getClausesReportsTheDocumentLanguagesOfTheCountryAndDerivedKind() {
    when(leaseKindResolver.resolveFor(any(), any(), eq(TEAM_ID))).thenReturn(LeaseKind.COMMERCIAL);
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contractIn(Optional.of("IT")));
    when(resolver.availabilityFor(any(), eq(LeaseKind.COMMERCIAL))).thenReturn(available());
    when(resolver.resolve(any(), any(), any(), anyList())).thenReturn(List.of());
    when(documentLanguageCatalog.availableLanguages("IT", LeaseKind.COMMERCIAL))
        .thenReturn(List.of("it", "en"));

    LeaseClausesResponse response = service.getClauses(CONTRACT_IDENTIFIER, principal);

    assertThat(response.documentLanguages()).containsExactly("it", "en");
  }

  @Test
  void getClausesOffersEveryLanguageWhenOnlyTheLegacyGenericTemplateApplies() {
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(available());
    when(resolver.resolve(any(), any(), any(), anyList())).thenReturn(List.of());
    when(documentLanguageCatalog.availableLanguages("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of());

    LeaseClausesResponse response = service.getClauses(CONTRACT_IDENTIFIER, principal);

    assertThat(response.documentLanguages()).containsExactlyElementsOf(DocumentLanguages.ORDERED);
  }

  @Test
  void updateClausesReportsTheDocumentLanguages() {
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(available());
    when(resolver.resolve(any(), any(), any(), anyList())).thenReturn(List.of());
    when(documentLanguageCatalog.availableLanguages("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of("nl", "en"));

    LeaseClausesResponse response =
        service.updateClauses(
            CONTRACT_IDENTIFIER, new UpdateContractLeaseClausesRequest(List.of()), principal);

    assertThat(response.documentLanguages()).containsExactly("nl", "en");
  }

  @Test
  void getClausesForUnsupportedCountryReturnsEmptyEnvelopeWithoutThrowing() {
    Contract it = contractIn(Optional.of("IT"));
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID)).thenReturn(it);
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(
            new LeaseClauseResolver.Availability(LeaseAvailability.UNAVAILABLE_COUNTRY, List.of()));

    LeaseClausesResponse response = service.getClauses(CONTRACT_IDENTIFIER, principal);

    assertThat(response.availability()).isEqualTo(LeaseAvailability.UNAVAILABLE_COUNTRY);
    assertThat(response.countryCode()).contains("IT");
    assertThat(response.clauses()).isEmpty();
    assertThat(response.documentLanguages()).isEmpty();
    verifyNoInteractions(documentLanguageCatalog);
    verify(resolver, never()).resolve(any(), any(), any(), anyList());
  }

  @Test
  void getClausesForContractWithoutCountryReturnsNoCountryEnvelope() {
    Contract noCountry = contractIn(Optional.empty());
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(noCountry);
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(
            new LeaseClauseResolver.Availability(
                LeaseAvailability.UNAVAILABLE_NO_COUNTRY, List.of()));

    LeaseClausesResponse response = service.getClauses(CONTRACT_IDENTIFIER, principal);

    assertThat(response.availability()).isEqualTo(LeaseAvailability.UNAVAILABLE_NO_COUNTRY);
    assertThat(response.countryCode()).isEmpty();
    assertThat(response.clauses()).isEmpty();
  }

  @Test
  void getClausesEnvelopeReportsThePropertysCountryWhenTheContractHasNone() {
    Property property = Property.builder().id(PROPERTY_ID).countryCode("be").build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contractIn(Optional.empty()));
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(
            new LeaseClauseResolver.Availability(LeaseAvailability.UNAVAILABLE_COUNTRY, List.of()));

    LeaseClausesResponse response = service.getClauses(CONTRACT_IDENTIFIER, principal);

    assertThat(response.countryCode()).contains("BE");
  }

  @Test
  void updateClausesDoesNotRejectAContractWhoseCountryComesFromTheProperty() {
    Property property = Property.builder().id(PROPERTY_ID).countryCode("NL").build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contractIn(Optional.empty()));
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(available());
    when(resolver.resolve(any(), any(), any(), anyList())).thenReturn(List.of());

    LeaseClausesResponse response =
        service.updateClauses(
            CONTRACT_IDENTIFIER, new UpdateContractLeaseClausesRequest(List.of()), principal);

    assertThat(response.countryCode()).contains("NL");
  }

  @Test
  void updateClausesForContractWithoutCountryThrowsNoCountryCodeBeforeAnyWrite() {
    Contract noCountry = contractIn(Optional.empty());
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(noCountry);

    assertThatThrownBy(
            () ->
                service.updateClauses(
                    CONTRACT_IDENTIFIER,
                    new UpdateContractLeaseClausesRequest(List.of()),
                    principal))
        .isInstanceOfSatisfying(
            LeaseNotAvailableException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("LEASE_CONTRACT_HAS_NO_COUNTRY"));

    verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any());
  }

  @Test
  void updateClausesWithBlankContractAndBlankPropertyCountryThrowsNoCountryBeforeAnyWrite() {
    Property blankProperty = Property.builder().id(PROPERTY_ID).countryCode("   ").build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(blankProperty);
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contractIn(Optional.of("  ")));

    assertThatThrownBy(
            () ->
                service.updateClauses(
                    CONTRACT_IDENTIFIER,
                    new UpdateContractLeaseClausesRequest(List.of()),
                    principal))
        .isInstanceOfSatisfying(
            LeaseNotAvailableException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("LEASE_CONTRACT_HAS_NO_COUNTRY"));

    verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any());
    verify(resolver, never()).availabilityFor(any(), any());
  }

  @Test
  void getClausesWithBlankContractAndBlankPropertyCountryPassesNoCountryToTheResolver() {
    Property blankProperty = Property.builder().id(PROPERTY_ID).countryCode("").build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(blankProperty);
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contractIn(Optional.of("  ")));
    when(resolver.availabilityFor(eq(Optional.empty()), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(
            new LeaseClauseResolver.Availability(
                LeaseAvailability.UNAVAILABLE_NO_COUNTRY, List.of()));

    LeaseClausesResponse response = service.getClauses(CONTRACT_IDENTIFIER, principal);

    assertThat(response.availability()).isEqualTo(LeaseAvailability.UNAVAILABLE_NO_COUNTRY);
    assertThat(response.countryCode()).isEmpty();
  }

  @Test
  void updateClausesForUnsupportedCountryThrowsCountryCodeBeforeAnyWrite() {
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(
            new LeaseClauseResolver.Availability(LeaseAvailability.UNAVAILABLE_COUNTRY, List.of()));

    assertThatThrownBy(
            () ->
                service.updateClauses(
                    CONTRACT_IDENTIFIER,
                    new UpdateContractLeaseClausesRequest(List.of()),
                    principal))
        .isInstanceOfSatisfying(
            LeaseNotAvailableException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("LEASE_NOT_AVAILABLE_FOR_COUNTRY"));

    verify(overrideRepository, never()).replaceForContract(any(), any(), any(), any());
  }

  @Test
  void requestSortOrderIsStoredVerbatimEvenForPinnedClauses() {
    Sid templateIdentifier = Sid.of("LCT0000000000000000000000001");
    LeaseClauseTemplate pinned = template(templateIdentifier, "parties", false, 1);
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL))).thenReturn(available(pinned));
    when(resolver.resolve(any(), any(), any(), anyList())).thenReturn(List.of());

    service.updateClauses(
        CONTRACT_IDENTIFIER,
        new UpdateContractLeaseClausesRequest(
            List.of(new ClauseSelection(templateIdentifier.value(), true, 99))),
        principal);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ContractLeaseClause>> captor = ArgumentCaptor.forClass(List.class);
    verify(overrideRepository).replaceForContract(any(), any(), any(), captor.capture());
    // Ignoring a pinned clause's position is the resolver's job (see LeaseClauseResolverTest).
    assertThat(captor.getValue().get(0).getSortOrder()).isEqualTo(99);
  }

  @Test
  void validRequestSucceedsAndReplacesOverrides() {
    Sid templateIdentifier = Sid.of("LCT0000000000000000000000001");
    LeaseClauseTemplate optionalClause =
        template(templateIdentifier, "house-rules", /* optional= */ true, 1);

    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract());
    when(resolver.availabilityFor(any(), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(available(optionalClause));

    List<ResolvedLeaseClauseResponse> resolved =
        List.of(
            new ResolvedLeaseClauseResponse(
                templateIdentifier, "house-rules", "Title", "Body", true, true, 1, false, 1));
    when(resolver.resolve(any(), any(), any(), anyList())).thenReturn(resolved);

    UpdateContractLeaseClausesRequest request =
        new UpdateContractLeaseClausesRequest(
            List.of(new ClauseSelection(templateIdentifier.value(), true, 1)));

    LeaseClausesResponse result = service.updateClauses(CONTRACT_IDENTIFIER, request, principal);

    assertThat(result.clauses()).isEqualTo(resolved);
    assertThat(result.availability()).isEqualTo(LeaseAvailability.AVAILABLE_DOCUMENT);
    assertThat(result.countryCode()).contains("NL");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<ContractLeaseClause>> captor = ArgumentCaptor.forClass(List.class);
    verify(overrideRepository)
        .replaceForContract(eq(CONTRACT_ID), eq(TEAM_ID), eq(USER_ID), captor.capture());

    List<ContractLeaseClause> saved = captor.getValue();
    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).getClauseTemplateId()).isEqualTo(optionalClause.getId());
    assertThat(saved.get(0).isIncluded()).isTrue();
    assertThat(saved.get(0).getSortOrder()).isEqualTo(1);

    // The already-fetched template list is passed into resolve(), not re-fetched by it.
    verify(resolver, times(1)).availabilityFor(any(), eq(LeaseKind.RESIDENTIAL));
    verify(resolver).resolve(any(), any(), any(), eq(List.of(optionalClause)));
  }

  @Test
  void getClausesWithRealResolverAgreesWithEnvelopeForLowerCaseStoredCountry() {
    var templateRepository = mock(com.buurman.repository.LeaseClauseTemplateRepository.class);
    var messageSource = mock(org.springframework.context.MessageSource.class);
    var realResolver =
        new LeaseClauseResolver(templateRepository, overrideRepository, messageSource);
    var realService =
        new LeaseClauseService(
            contractRepository,
            overrideRepository,
            realResolver,
            propertyRepository,
            leaseKindResolver,
            documentLanguageCatalog);
    LeaseClauseTemplate t = template(Sid.of("LCT0000000000000000000000001"), "parties", false, 1);
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contractIn(Optional.of("nl")));
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(t));
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(messageSource.getMessage(any(), any(), any(java.util.Locale.class))).thenReturn("text");

    LeaseClausesResponse response = realService.getClauses(CONTRACT_IDENTIFIER, principal);

    assertThat(response.availability()).isEqualTo(LeaseAvailability.AVAILABLE_DOCUMENT);
    assertThat(response.countryCode()).contains("NL");
    assertThat(response.clauses()).hasSize(1);
    verify(propertyRepository, times(1)).getByIdAndTeamId(PROPERTY_ID, TEAM_ID);
  }
}
