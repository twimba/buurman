package com.buurman.service.letters;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractRentComponent;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.RentComponentType;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitResidentialDetails;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.LeaseNotAvailableException;
import com.buurman.repository.ContractRentComponentRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.service.LeaseClauseResolver;
import com.buurman.service.LeaseKindResolver;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

/**
 * Mockito unit test (no Spring context, mirrors {@code SignatureServiceTest}'s style). Proves the
 * behaviors that matter most for this exporter: only clauses the resolver marked {@code included}
 * ever reach the template's variable map, regeneration is never cached (a second, different
 * resolution produces a different {@code clauses} list), and an all-excluded resolution throws
 * before the template service is ever touched.
 */
class LeaseAgreementExporterTest {

  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final ContractRentComponentRepository rentComponentRepository =
      mock(ContractRentComponentRepository.class);
  private final ContractRentPeriodRepository rentPeriodRepository =
      mock(ContractRentPeriodRepository.class);
  private final LeaseClauseResolver clauseResolver = mock(LeaseClauseResolver.class);
  private final UnitResidentialDetailsRepository unitDetailsRepository =
      mock(UnitResidentialDetailsRepository.class);
  private final LeaseKindResolver leaseKindResolver = new LeaseKindResolver(unitDetailsRepository);
  private final LeaseDocumentLocator documentLocator = mock(LeaseDocumentLocator.class);
  private final TeamRepository teamRepository = mock(TeamRepository.class);
  private final LetterExporterHelper helper = mock(LetterExporterHelper.class);
  private final LetterTemplateService documentTemplateService = mock(LetterTemplateService.class);
  private final MessageSource messageSource = mock(MessageSource.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

  private LeaseAgreementExporter exporter;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final ContractIdentifier CONTRACT_IDENTIFIER =
      ContractIdentifier.of("CON00000000000000000000001");

  private Contract contract;
  private Property property;

  @BeforeEach
  void setUp() {
    exporter =
        new LeaseAgreementExporter(
            contractRepository,
            propertyRepository,
            rentComponentRepository,
            rentPeriodRepository,
            clauseResolver,
            leaseKindResolver,
            documentLocator,
            teamRepository,
            helper,
            documentTemplateService,
            messageSource,
            clock);

    contract =
        Contract.builder()
            .id(CONTRACT_ID)
            .teamId(TEAM_ID)
            .propertyId(PROPERTY_ID)
            .identifier(Optional.of(Sid.of(CONTRACT_IDENTIFIER.value())))
            .countryCode(Optional.of("NL"))
            .contractType(Contract.ContractType.FIXED_TERM)
            .startDate(LocalDate.of(2026, 2, 1))
            .rentAmount(new MoneyAmount(new BigDecimal("1000.00"), "EUR"))
            .paymentFrequency(Contract.PaymentFrequency.MONTHLY)
            .build();
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract);

    property =
        Property.builder()
            .id(PROPERTY_ID)
            .street("Keizersgracht 12")
            .postalCode("1015 CJ")
            .city("Amsterdam")
            .build();
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);

    Unit unit = Unit.builder().unitNumber("1").name(Optional.empty()).build();
    LetterExporterHelper.PremisesInfo premisesInfo =
        new LetterExporterHelper.PremisesInfo(unit, 1, "unit ");
    when(helper.premisesInfo(eq(contract), eq(property), eq(messageSource), any(Locale.class)))
        .thenReturn(premisesInfo);

    LetterExporterHelper.Addressee addressee =
        new LetterExporterHelper.Addressee(Optional.empty(), Optional.empty());
    when(helper.addressee(CONTRACT_ID, TEAM_ID)).thenReturn(addressee);

    when(teamRepository.getById(TEAM_ID)).thenReturn(Team.builder().name("Landlord BV").build());
    when(helper.loadPartyData(CONTRACT_ID, TEAM_ID))
        .thenReturn(new LetterExporterHelper.PartyData(List.of(), Map.of()));
    when(helper.buildContactNamesList(any(), any())).thenReturn("Tenant One");
    when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("label");
    when(documentLocator.locate(anyString(), any(LeaseKind.class), anyString()))
        .thenReturn(Optional.empty());

    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of());

    when(helper.signatureBlocks(
            eq(CONTRACT_ID), eq(TEAM_ID), eq(messageSource), anyString(), any(Locale.class)))
        .thenReturn(
            List.of(
                Map.of(
                    "label", "Landlord / Property Manager", "placeholder", "signature-landlord")));

    when(documentTemplateService.renderToPdf(anyString(), any(Locale.class), anyMap()))
        .thenReturn("%PDF-1.7\nstub".getBytes(UTF_8));
  }

  private static ResolvedLeaseClauseResponse clause(
      String key, String title, String body, boolean included) {
    return new ResolvedLeaseClauseResponse(
        Sid.of("LCT0000000000000000000000" + (included ? "1" : "0")),
        key,
        title,
        body,
        included,
        true,
        0,
        false,
        included ? 1 : 0);
  }

  @Test
  @DisplayName("a contract without a unit resolves the kind without any unit-details lookup")
  void nullUnitIdSkipsUnitDetailsLookup() {
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(clause("term", "Term", "Body", true)));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    verifyNoInteractions(unitDetailsRepository);
  }

  @Test
  @DisplayName("a furnished unit makes the exporter ask the resolver for the furnished kind")
  void furnishedUnitResolvesFurnishedKind() {
    UUID unitId = UUID.randomUUID();
    contract.setUnitId(unitId);
    when(unitDetailsRepository.findByUnitIdAndTeamId(unitId, TEAM_ID))
        .thenReturn(Optional.of(UnitResidentialDetails.builder().furnished(true).build()));
    when(clauseResolver.resolve(
            eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL_FURNISHED)))
        .thenReturn(List.of(clause("term", "Term", "Body", true)));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    verify(clauseResolver)
        .resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL_FURNISHED));
  }

  @Test
  @DisplayName("only included clauses reach the renderToPdf variable map")
  void onlyIncludedClausesReachTemplate() {
    ResolvedLeaseClauseResponse included1 =
        clause("term", "Term of Lease", "The lease runs for twelve months.", true);
    ResolvedLeaseClauseResponse excluded = clause("pets", "Pets", "Pets are not permitted.", false);
    ResolvedLeaseClauseResponse included2 =
        clause("maintenance", "Maintenance", "Tenant handles minor repairs.", true);
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(included1, excluded, included2));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService)
        .renderToPdf(eq("lease-agreement"), any(Locale.class), captor.capture());

    Object clausesObj = captor.getValue().get("clauses");
    assertThat(clausesObj).isInstanceOf(List.class);
    @SuppressWarnings("unchecked")
    List<Map<String, String>> clauses = (List<Map<String, String>>) clausesObj;

    assertThat(clauses).hasSize(2);
    assertThat(clauses)
        .extracting(c -> c.get("title"), c -> c.get("body"))
        .containsExactly(
            org.assertj.core.api.Assertions.tuple(
                "Term of Lease", "The lease runs for twelve months."),
            org.assertj.core.api.Assertions.tuple("Maintenance", "Tenant handles minor repairs."));
    assertThat(clauses).noneMatch(c -> c.get("title").equals("Pets"));
  }

  @Test
  @DisplayName(
      "regenerating after a clause selection change is never cached — a second, different"
          + " resolution produces a different clauses list")
  void regenerationIsNotCached() {
    ResolvedLeaseClauseResponse firstSelectionClause =
        clause("term", "Term of Lease", "The lease runs for twelve months.", true);
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(firstSelectionClause));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    // Landlord flips a clause selection, then regenerates: the resolver now returns a
    // different set of included clauses for the very same contract.
    ResolvedLeaseClauseResponse secondSelectionClause =
        clause("pets", "Pets", "Pets are permitted with a deposit.", true);
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(secondSelectionClause));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService, org.mockito.Mockito.times(2))
        .renderToPdf(eq("lease-agreement"), any(Locale.class), captor.capture());

    List<Map<String, Object>> allVariables = captor.getAllValues();
    @SuppressWarnings("unchecked")
    List<Map<String, String>> firstClauses =
        (List<Map<String, String>>) (List<?>) allVariables.get(0).get("clauses");
    @SuppressWarnings("unchecked")
    List<Map<String, String>> secondClauses =
        (List<Map<String, String>>) (List<?>) allVariables.get(1).get("clauses");

    assertThat(firstClauses).extracting(c -> c.get("title")).containsExactly("Term of Lease");
    assertThat(secondClauses).extracting(c -> c.get("title")).containsExactly("Pets");
    assertThat(firstClauses).isNotEqualTo(secondClauses);
  }

  @Test
  @DisplayName("an unavailable country surfaces LeaseNotAvailableException so generate maps to 409")
  void unavailableCountryPropagatesTypedException() {
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenThrow(LeaseNotAvailableException.forCountry("IT"));

    assertThatThrownBy(() -> exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en"))
        .isInstanceOfSatisfying(
            LeaseNotAvailableException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("LEASE_NOT_AVAILABLE_FOR_COUNTRY"));
    verifyNoInteractions(documentTemplateService);
  }

  @Test
  @DisplayName(
      "throws BusinessRuleException before renderToPdf is ever called when no clauses are"
          + " included")
  void emptyIncludedClausesThrowsBeforeRender() {
    ResolvedLeaseClauseResponse excludedOnly =
        clause("pets", "Pets", "Pets are not permitted.", false);
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(excludedOnly));

    assertThatThrownBy(() -> exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en"))
        .isInstanceOf(BusinessRuleException.class);

    verifyNoInteractions(documentTemplateService);
  }

  @Test
  @DisplayName(
      "renders the shell with typed values, refs and language flags when a document exists")
  void shellPathUsedWhenDocumentExists() {
    when(documentLocator.locate("NL", LeaseKind.RESIDENTIAL, "en"))
        .thenReturn(
            Optional.of(
                new LeaseDocumentLocator.LeaseDocument(
                    "lease-agreement/NL/residential/nl", "nl", true)));
    when(documentTemplateService.renderToPdfTemplate(anyString(), any(Locale.class), anyMap()))
        .thenReturn("%PDF".getBytes(UTF_8));
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(
            List.of(
                clause("term", "Term", "Body", true),
                clause("pets", "Pets", "Body", false),
                clause("rent", "Rent", "Body", true)));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService)
        .renderToPdfTemplate(eq("lease-agreement/_shell"), any(Locale.class), captor.capture());
    verify(documentTemplateService, org.mockito.Mockito.never())
        .renderToPdf(anyString(), any(Locale.class), anyMap());
    Map<String, Object> vars = captor.getValue();
    assertThat(vars)
        .containsEntry("clauseSource", "lease-agreement/NL/residential/nl")
        .containsEntry("languageUsed", "nl")
        .containsEntry("requestedLang", "en")
        .containsEntry("fallbackUsed", true)
        .containsEntry("authoritative", true)
        .containsEntry("landlordName", "Landlord BV")
        .containsEntry("tenantNames", "Tenant One")
        .containsEntry("landlordNoticeDays", 30)
        .containsEntry("fixedTerm", true)
        .containsKey("startDate");
    assertThat((Map<String, Integer>) vars.get("refs")).containsOnlyKeys("term", "rent");
    assertThat((List<?>) vars.get("clauses")).hasSize(2);
  }

  @Test
  @DisplayName("fixedTerm follows the contract type, not the presence of an end date")
  void fixedTermFollowsContractType() {
    contract.setContractType(Contract.ContractType.INDEFINITE);
    when(documentLocator.locate("NL", LeaseKind.RESIDENTIAL, "nl"))
        .thenReturn(
            Optional.of(
                new LeaseDocumentLocator.LeaseDocument(
                    "lease-agreement/NL/residential/nl", "nl", true)));
    when(documentTemplateService.renderToPdfTemplate(anyString(), any(Locale.class), anyMap()))
        .thenReturn("%PDF".getBytes(UTF_8));
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(clause("term", "Term", "Body", true)));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "nl");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService)
        .renderToPdfTemplate(eq("lease-agreement/_shell"), any(Locale.class), captor.capture());
    assertThat(captor.getValue()).containsEntry("fixedTerm", false);
  }

  @Test
  @DisplayName("all clauses excluded throws before any rendering on the shell path too")
  void allExcludedThrowsOnShellPathToo() {
    when(documentLocator.locate("NL", LeaseKind.RESIDENTIAL, "en"))
        .thenReturn(
            Optional.of(
                new LeaseDocumentLocator.LeaseDocument(
                    "lease-agreement/NL/residential/en", "en", false)));
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(clause("pets", "Pets", "Body", false)));

    assertThatThrownBy(() -> exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en"))
        .isInstanceOf(BusinessRuleException.class);
    verifyNoInteractions(documentTemplateService);
  }

  @Test
  @DisplayName("fallback renders the whole PDF in the located language's locale")
  void fallbackRendersWholePdfInLocatedLocale() {
    when(documentLocator.locate("NL", LeaseKind.RESIDENTIAL, "en"))
        .thenReturn(
            Optional.of(
                new LeaseDocumentLocator.LeaseDocument(
                    "lease-agreement/NL/residential/nl", "nl", true)));
    when(documentTemplateService.renderToPdfTemplate(anyString(), any(Locale.class), anyMap()))
        .thenReturn("%PDF".getBytes(UTF_8));
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(clause("term", "Term", "Body", true)));

    LeaseAgreementExporter.RenderedLease rendered =
        exporter.generateWithLanguage(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    assertThat(rendered.languageUsed()).isEqualTo("nl");
    verify(clauseResolver)
        .resolve(eq(contract), eq(Locale.forLanguageTag("nl")), eq(LeaseKind.RESIDENTIAL));
    verify(documentTemplateService)
        .renderToPdfTemplate(
            eq("lease-agreement/_shell"), eq(Locale.forLanguageTag("nl")), anyMap());
  }

  @Test
  @DisplayName("an unsafe clause key is rejected before rendering on the shell path")
  void unsafeClauseKeyRejected() {
    when(documentLocator.locate("NL", LeaseKind.RESIDENTIAL, "en"))
        .thenReturn(
            Optional.of(
                new LeaseDocumentLocator.LeaseDocument(
                    "lease-agreement/NL/residential/en", "en", false)));
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(clause("a::b", "Bad", "Body", true)));

    assertThatThrownBy(() -> exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en"))
        .isInstanceOf(BusinessRuleException.class);
    verifyNoInteractions(documentTemplateService);
  }

  @Test
  @DisplayName("legacy path adds no typed values and does no team or party lookups")
  void legacyPathHasNoTypedValues() {
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(clause("term", "Term", "Body", true)));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService)
        .renderToPdf(eq("lease-agreement"), any(Locale.class), captor.capture());
    assertThat(captor.getValue())
        .doesNotContainKeys("landlordName", "tenantNames", "startDate", "rentAmount", "refs");
    verifyNoInteractions(teamRepository);
  }

  private static final Locale NL = Locale.forLanguageTag("nl");

  private static MoneyAmount eur(String value) {
    return new MoneyAmount(new BigDecimal(value), "EUR");
  }

  private static String money(String value) {
    return CurrencyUtils.formatCurrency(new BigDecimal(value), "EUR", NL);
  }

  private static ContractRentComponent component(
      RentComponentType type, String amount, UUID periodId) {
    return ContractRentComponent.builder()
        .componentType(type)
        .amount(eur(amount))
        .rentPeriodId(periodId)
        .build();
  }

  private Map<String, Object> renderShellVariables() {
    when(documentLocator.locate("NL", LeaseKind.RESIDENTIAL, "nl"))
        .thenReturn(
            Optional.of(
                new LeaseDocumentLocator.LeaseDocument(
                    "lease-agreement/NL/residential/nl", "nl", true)));
    when(documentTemplateService.renderToPdfTemplate(anyString(), any(Locale.class), anyMap()))
        .thenReturn("%PDF".getBytes(UTF_8));
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(clause("rent", "Rent", "Body", true)));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "nl");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService)
        .renderToPdfTemplate(eq("lease-agreement/_shell"), any(Locale.class), captor.capture());
    return captor.getValue();
  }

  @Test
  @DisplayName("rentAmount is the BASE_RENT sum, not the sum of all components")
  void rentAmountIsBaseRentOnly() {
    contract.setRentAmount(eur("1150.00"));
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                component(RentComponentType.BASE_RENT, "1000.00", null),
                component(RentComponentType.SERVICE_COSTS, "150.00", null)));

    Map<String, Object> vars = renderShellVariables();

    assertThat(vars).containsEntry("rentAmount", money("1000.00"));
    assertThat(vars.get("rentAmount")).isNotEqualTo(money("1150.00"));
  }

  @Test
  @DisplayName("several BASE_RENT components are summed")
  void severalBaseRentComponentsAreSummed() {
    contract.setRentAmount(eur("1400.00"));
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                component(RentComponentType.BASE_RENT, "900.00", null),
                component(RentComponentType.BASE_RENT, "100.00", null),
                component(RentComponentType.PARKING, "400.00", null)));

    assertThat(renderShellVariables()).containsEntry("rentAmount", money("1000.00"));
  }

  @Test
  @DisplayName("a contract without components keeps its own rentAmount")
  void noComponentsKeepsContractRent() {
    assertThat(renderShellVariables()).containsEntry("rentAmount", money("1000.00"));
  }

  @Test
  @DisplayName("components without any BASE_RENT fall back to the contract rentAmount")
  void componentsWithoutBaseRentFallBack() {
    contract.setRentAmount(eur("1150.00"));
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(component(RentComponentType.SERVICE_COSTS, "150.00", null)));

    assertThat(renderShellVariables()).containsEntry("rentAmount", money("1150.00"));
  }

  @Test
  @DisplayName("with several rent periods only the active period's components count")
  void onlyActivePeriodComponentsCount() {
    UUID oldPeriod = UUID.randomUUID();
    UUID currentPeriod = UUID.randomUUID();
    contract.setRentAmount(eur("1250.00"));
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                component(RentComponentType.BASE_RENT, "1000.00", oldPeriod),
                component(RentComponentType.BASE_RENT, "1100.00", currentPeriod),
                component(RentComponentType.SERVICE_COSTS, "150.00", currentPeriod)));
    when(rentPeriodRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                ContractRentPeriod.builder()
                    .id(oldPeriod)
                    .effectiveFrom(LocalDate.of(2025, 1, 1))
                    .effectiveTo(Optional.of(LocalDate.of(2025, 12, 31)))
                    .build(),
                ContractRentPeriod.builder()
                    .id(currentPeriod)
                    .effectiveFrom(LocalDate.of(2026, 1, 1))
                    .build()));

    assertThat(renderShellVariables()).containsEntry("rentAmount", money("1100.00"));
  }

  @Test
  @DisplayName("deposit falls back to securityDeposit when depositAmount is unset")
  void depositFallsBackToSecurityDeposit() {
    contract.setSecurityDeposit(Optional.of(eur("2000.00")));

    assertThat(renderShellVariables()).containsEntry("depositAmount", money("2000.00"));
  }

  @Test
  @DisplayName("depositAmount wins when both deposits are set")
  void depositAmountWins() {
    contract.setDepositAmount(Optional.of(eur("1800.00")));
    contract.setSecurityDeposit(Optional.of(eur("2000.00")));

    assertThat(renderShellVariables()).containsEntry("depositAmount", money("1800.00"));
  }

  @Test
  @DisplayName("no deposit at all yields null")
  void noDeposit() {
    assertThat(renderShellVariables()).containsEntry("depositAmount", null);
  }

  @Test
  @DisplayName("rent component labels come from the document-locale bundle, not the enum name")
  void rentComponentLabelsAreLocalized() {
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(component(RentComponentType.SERVICE_COSTS, "150.00", null)));
    when(messageSource.getMessage(eq("lease.rentComponent.SERVICE_COSTS"), any(), eq(NL)))
        .thenReturn("Servicekosten");

    @SuppressWarnings("unchecked")
    List<Map<String, String>> rows =
        (List<Map<String, String>>) (List<?>) renderShellVariables().get("rentComponents");

    assertThat(rows).extracting(r -> r.get("type")).containsExactly("Servicekosten");
  }

  @Test
  @DisplayName("legacy path also localizes the rent component labels")
  void legacyPathLocalizesRentComponentLabels() {
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(component(RentComponentType.PARKING, "50.00", null)));
    when(messageSource.getMessage(eq("lease.rentComponent.PARKING"), any(), any(Locale.class)))
        .thenReturn("Parking-label");
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(List.of(clause("term", "Term", "Body", true)));

    exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "en");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(documentTemplateService).renderToPdf(eq("lease-agreement"), any(), captor.capture());
    assertThat((List<Map<String, String>>) (List<?>) captor.getValue().get("rentComponents"))
        .extracting(r -> r.get("type"))
        .containsExactly("Parking-label");
  }

  @Test
  @DisplayName("a clause key without a fragment in the located document fails before rendering")
  void clauseWithoutFragmentFailsBeforeRender() {
    when(documentLocator.locate("NL", LeaseKind.RESIDENTIAL, "nl"))
        .thenReturn(
            Optional.of(
                new LeaseDocumentLocator.LeaseDocument(
                    "lease-agreement/NL/residential/nl", "nl", true)));
    when(clauseResolver.resolve(eq(contract), any(Locale.class), eq(LeaseKind.RESIDENTIAL)))
        .thenReturn(
            List.of(clause("rent", "Rent", "Body", true), clause("pets", "Pets", "Body", true)));

    assertThatThrownBy(() -> exporter.generate(CONTRACT_IDENTIFIER, TEAM_ID, "nl"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("pets")
        .hasMessageContaining("lease-agreement/NL/residential/nl");
    verifyNoInteractions(documentTemplateService);
  }
}
