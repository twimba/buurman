package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import com.buurman.domain.Contract;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.LeaseNotAvailableException;
import com.buurman.repository.ContractRentComponentRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.service.LeaseClauseResolver;
import com.buurman.service.LeaseKindResolver;
import com.buurman.util.MoneyAmount;

/** The seams the preview uses (plan, loadInput, assemble) and the ordering the real path keeps. */
@DisplayName("lease exporter seams")
class LeaseAgreementSeamsTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID PROPERTY_ID = UUID.randomUUID();
  private static final ContractIdentifier IDENTIFIER =
      ContractIdentifier.of("CON00000000000000000000001");

  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final ContractRentComponentRepository rentComponents =
      mock(ContractRentComponentRepository.class);
  private final ContractRentPeriodRepository rentPeriods = mock(ContractRentPeriodRepository.class);
  private final LeaseClauseResolver resolver = mock(LeaseClauseResolver.class);
  private final LeaseKindResolver kindResolver = mock(LeaseKindResolver.class);
  private final LeaseDocumentLocator locator = mock(LeaseDocumentLocator.class);
  private final TeamRepository teamRepository = mock(TeamRepository.class);
  private final LetterExporterHelper helper = mock(LetterExporterHelper.class);
  private final MessageSource messageSource = mock(MessageSource.class);

  private LeaseAgreementExporter exporter;
  private Contract contract;
  private Property property;

  @BeforeEach
  void setUp() {
    exporter =
        new LeaseAgreementExporter(
            contractRepository,
            propertyRepository,
            rentComponents,
            rentPeriods,
            resolver,
            kindResolver,
            locator,
            teamRepository,
            helper,
            mock(LetterTemplateService.class),
            messageSource,
            CLOCK);
    contract =
        Contract.builder()
            .id(CONTRACT_ID)
            .teamId(TEAM_ID)
            .propertyId(PROPERTY_ID)
            .unitId(UUID.randomUUID())
            .identifier(Optional.of(Sid.of(IDENTIFIER.value())))
            .countryCode(Optional.of("NL"))
            .contractType(Contract.ContractType.INDEFINITE)
            .startDate(LocalDate.of(2026, 2, 1))
            .rentAmount(new MoneyAmount(new BigDecimal("1000.00"), "EUR"))
            .paymentFrequency(Contract.PaymentFrequency.MONTHLY)
            .build();
    property =
        Property.builder()
            .id(PROPERTY_ID)
            .street("Street 1")
            .postalCode("1000 AA")
            .city("City")
            .build();
    when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");
    when(helper.addressee(CONTRACT_ID, TEAM_ID))
        .thenReturn(new LetterExporterHelper.Addressee(Optional.empty(), Optional.empty()));
    when(helper.premisesInfo(any(), any(), any(), any()))
        .thenReturn(
            new LetterExporterHelper.PremisesInfo(
                Unit.builder().unitNumber("1").name(Optional.empty()).build(), 1, "unit "));
    when(helper.signatureBlocks(any(), any(), any(), anyString(), any())).thenReturn(List.of());
  }

  private static LeaseClauseTemplate template(String key) {
    return LeaseClauseTemplate.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(Sid.of("LCT00000000000000000000001")))
        .countryCode("NL")
        .leaseKind(LeaseKind.RESIDENTIAL)
        .clauseKey(key)
        .titleI18nKey("t")
        .bodyI18nKey("b")
        .defaultIncluded(true)
        .optional(false)
        .sortOrder(1)
        .version(1)
        .build();
  }

  private static ResolvedLeaseClauseResponse resolved(String key, boolean included) {
    return new ResolvedLeaseClauseResponse(
        Sid.of("LCT00000000000000000000001"), key, "T", "B", included, !included, 1, false, 1);
  }

  private LeaseAgreementExporter.LeaseRenderPlan shellPlan(String templateKey) {
    when(resolver.templatesFor(Optional.of("NL"), LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(template(templateKey)));
    when(locator.locate("NL", LeaseKind.RESIDENTIAL, "en"))
        .thenReturn(
            Optional.of(
                new LeaseDocumentLocator.LeaseDocument(
                    "lease-agreement/NL/residential/en", "en", true)));
    return exporter.plan(Optional.of("NL"), LeaseKind.RESIDENTIAL, "en");
  }

  @Test
  @DisplayName("plan exposes the kind whose templates apply and the located document")
  void planExposesKindAndDocument() {
    var plan = shellPlan("rent");

    assertThat(plan.kindUsed()).isEqualTo(LeaseKind.RESIDENTIAL);
    assertThat(plan.legacy()).isFalse();
    assertThat(plan.languageUsed()).isEqualTo("en");
    assertThat(plan.locale()).isEqualTo(Locale.ENGLISH);
  }

  @Test
  @DisplayName("plan is legacy when no per-language document exists and fails for no templates")
  void planLegacyAndUnavailable() {
    when(resolver.templatesFor(Optional.of("BE"), LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(template("rent")));
    when(locator.locate("BE", LeaseKind.RESIDENTIAL, "fr")).thenReturn(Optional.empty());
    assertThat(exporter.plan(Optional.of("BE"), LeaseKind.RESIDENTIAL, "fr").legacy()).isTrue();

    when(resolver.templatesFor(Optional.of("IT"), LeaseKind.RESIDENTIAL))
        .thenThrow(LeaseNotAvailableException.forCountry("IT"));
    assertThatThrownBy(() -> exporter.plan(Optional.of("IT"), LeaseKind.RESIDENTIAL, "en"))
        .isInstanceOf(LeaseNotAvailableException.class);
  }

  @Test
  @DisplayName("a document without a fragment for an included clause fails before loadInput")
  void fragmentCheckRunsBeforeAnyRepositoryAccess() {
    when(kindResolver.resolveFor(contract, property, TEAM_ID)).thenReturn(LeaseKind.RESIDENTIAL);
    when(contractRepository.getByIdentifierAndTeamId(IDENTIFIER, TEAM_ID)).thenReturn(contract);
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(property);
    var plan = shellPlan("not-a-real-fragment");
    when(resolver.resolve(
            eq(contract), eq(Optional.of("NL")), any(Locale.class), eq(plan.templates())))
        .thenReturn(List.of(resolved("not-a-real-fragment", true)));

    assertThatThrownBy(() -> exporter.generateWithLanguage(IDENTIFIER, TEAM_ID, "en"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("has no fragment for clause 'not-a-real-fragment'");
    verifyNoInteractions(teamRepository, helper, rentComponents, rentPeriods);
  }

  @Test
  @DisplayName("validateShell rejects an invalid key and ignores the legacy path")
  void validateShell() {
    var shell = shellPlan("rent");
    assertThatThrownBy(
            () -> LeaseAgreementExporter.validateShell(shell, List.of(resolved("Bad_Key", true))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("invalid key");

    when(resolver.templatesFor(Optional.of("BE"), LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(template("rent")));
    when(locator.locate("BE", LeaseKind.RESIDENTIAL, "en")).thenReturn(Optional.empty());
    var legacy = exporter.plan(Optional.of("BE"), LeaseKind.RESIDENTIAL, "en");
    LeaseAgreementExporter.validateShell(legacy, List.of(resolved("Bad_Key", true)));
  }

  @Test
  @DisplayName("loadInput on the legacy path loads no landlord or tenant names")
  void loadInputLegacy() {
    when(resolver.templatesFor(Optional.of("BE"), LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(template("rent")));
    when(locator.locate("BE", LeaseKind.RESIDENTIAL, "en")).thenReturn(Optional.empty());
    var plan = exporter.plan(Optional.of("BE"), LeaseKind.RESIDENTIAL, "en");
    when(rentComponents.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());

    LeaseRenderInput input = exporter.loadInput(contract, property, TEAM_ID, plan);

    assertThat(input.landlordName()).isEmpty();
    assertThat(input.tenantNames()).isEmpty();
    assertThat(input.contractIdentifier()).isEqualTo(IDENTIFIER.value());
    assertThat(input.today()).isEqualTo(LocalDate.of(2026, 1, 15));
    assertThat(input.baseRent().value()).isEqualByComparingTo("1000.00");
    verifyNoInteractions(teamRepository);
  }

  @Test
  @DisplayName("loadInput on the document path loads the landlord and tenant names")
  void loadInputShell() {
    var plan = shellPlan("rent");
    when(rentComponents.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(teamRepository.getById(TEAM_ID)).thenReturn(Team.builder().name("Landlord BV").build());
    when(helper.loadPartyData(CONTRACT_ID, TEAM_ID))
        .thenReturn(new LetterExporterHelper.PartyData(List.of(), Map.of()));
    when(helper.buildContactNamesList(List.of(), Map.of())).thenReturn("A, B");

    LeaseRenderInput input = exporter.loadInput(contract, property, TEAM_ID, plan);

    assertThat(input.landlordName()).contains("Landlord BV");
    assertThat(input.tenantNames()).contains("A, B");
  }

  @Test
  @DisplayName("assembling a document without landlord or tenant names fails clearly")
  void assembleRequiresNames() {
    var plan = shellPlan("rent");
    LeaseRenderInput noNames = input(Optional.empty(), Optional.empty(), Optional.empty());

    assertThatThrownBy(
            () ->
                exporter.assembleResolved(
                    plan,
                    noNames,
                    List.of(resolved("rent", true)),
                    List.of(resolved("rent", true))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("landlordName is required");
  }

  @Test
  @DisplayName("assembleWithOverrides returns the full resolved list including excluded clauses")
  void assembleWithOverridesKeepsExcluded() {
    when(resolver.templatesFor(Optional.of("BE"), LeaseKind.RESIDENTIAL))
        .thenReturn(List.of(template("rent")));
    when(locator.locate("BE", LeaseKind.RESIDENTIAL, "en")).thenReturn(Optional.empty());
    var plan = exporter.plan(Optional.of("BE"), LeaseKind.RESIDENTIAL, "en");
    List<ResolvedLeaseClauseResponse> all =
        List.of(resolved("rent", true), resolved("deposit", false));
    when(resolver.resolve(eq(plan.templates()), any(), eq(plan.country()), eq(plan.locale())))
        .thenReturn(all);

    var assembled =
        exporter.assembleWithOverrides(
            plan, input(Optional.empty(), Optional.empty(), Optional.empty()), List.of());

    assertThat(assembled.clauses()).isEqualTo(all);
    assertThat(assembled.legacy()).isTrue();
    assertThat((List<?>) assembled.variables().get("clauses")).hasSize(1);
  }

  @Test
  @DisplayName("LeaseRenderInput copies its lists and tolerates null address fields")
  void inputIsDefensivelyImmutable() {
    Map<String, String> address = new HashMap<>();
    address.put("street", null);
    address.put("city", "Amsterdam");
    List<LeaseRenderInput.RentLine> lines = new java.util.ArrayList<>();

    LeaseRenderInput input = input(Optional.empty(), Optional.empty(), Optional.of(address));
    lines.add(new LeaseRenderInput.RentLine(null, null));

    assertThat(input.contactAddress().orElseThrow()).containsEntry("city", "Amsterdam");
    assertThatThrownBy(() -> input.contactAddress().orElseThrow().put("x", "y"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> input.rentComponents().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> input.signatureBlocks().add(Map.of()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static LeaseRenderInput input(
      Optional<String> landlord, Optional<String> tenants, Optional<Map<String, String>> address) {
    return new LeaseRenderInput(
        "SAMPLE",
        LocalDate.of(2026, 1, 15),
        Optional.empty(),
        LocalDate.of(2026, 2, 1),
        Optional.empty(),
        Contract.ContractType.INDEFINITE,
        Contract.PaymentFrequency.MONTHLY,
        30,
        30,
        new java.util.ArrayList<>(),
        new MoneyAmount(new BigDecimal("1000.00"), "EUR"),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        landlord,
        tenants,
        Optional.empty(),
        address,
        "Street 1",
        new java.util.ArrayList<>());
  }
}
