package com.buurman.service.letters;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;

import com.buurman.document.DocumentRenderer;
import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractLeaseClause;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.ContractRentComponent;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.RentComponentType;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractLeaseClauseRepository;
import com.buurman.repository.ContractRentComponentRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.LeaseClauseTemplateRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.LeaseClauseResolver;
import com.buurman.service.LeaseKindResolver;
import com.buurman.util.MoneyAmount;

/**
 * Golden-master safety net for the lease render pipeline. Everything between the repositories and
 * the PDF renderer is real (resolver, locator, helper, message bundles, Thymeleaf templates); only
 * repositories and the PDF engine are mocked. Each scenario asserts the exact template variable map
 * and the whitespace-normalised rendered HTML against files under {@code
 * src/test/resources/lease-characterization}. Regenerate deliberately with {@code
 * LEASE_GOLDEN_WRITE=1}.
 */
@DisplayName("lease render characterization (golden master)")
class LeaseRenderCharacterizationTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);
  private static final UUID TEAM_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
  private static final UUID CONTRACT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
  private static final UUID PROPERTY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
  private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
  private static final ContractIdentifier CONTRACT_IDENTIFIER =
      ContractIdentifier.of("CON00000000000000000000001");
  private static final Path GOLDEN_DIR = Path.of("src/test/resources/lease-characterization");

  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final ContractRentComponentRepository rentComponentRepository =
      mock(ContractRentComponentRepository.class);
  private final ContractRentPeriodRepository rentPeriodRepository =
      mock(ContractRentPeriodRepository.class);
  private final LeaseClauseTemplateRepository templateRepository =
      mock(LeaseClauseTemplateRepository.class);
  private final ContractLeaseClauseRepository overrideRepository =
      mock(ContractLeaseClauseRepository.class);
  private final TeamRepository teamRepository = mock(TeamRepository.class);
  private final ContractPartyService contractPartyService = mock(ContractPartyService.class);
  private final ContactRepository contactRepository = mock(ContactRepository.class);
  private final ContactAddressRepository contactAddressRepository =
      mock(ContactAddressRepository.class);
  private final UnitRepository unitRepository = mock(UnitRepository.class);
  private final UnitResidentialDetailsRepository unitDetailsRepository =
      mock(UnitResidentialDetailsRepository.class);
  private final DocumentRenderer pdfRenderer = mock(DocumentRenderer.class);
  private final AtomicReference<String> renderedHtml = new AtomicReference<>();

  private LetterTemplateService templateService;
  private LeaseAgreementExporter exporter;

  @BeforeEach
  void setUp() {
    MessageSource messageSource =
        DocumentTemplateSupport.messageSource(
            false,
            "classpath:messages/document-letter-chrome",
            "classpath:messages/document-extension",
            "classpath:messages/document-rent-change",
            "classpath:messages/document-payment-notice",
            "classpath:messages/document-deposit-statement",
            "classpath:messages/document-contract-termination",
            "classpath:messages/document-lease-agreement");
    when(pdfRenderer.render(anyString(), any()))
        .thenAnswer(
            inv -> {
              renderedHtml.set(inv.getArgument(0, String.class));
              return "%PDF-1.7\nstub".getBytes(UTF_8);
            });
    templateService =
        spy(
            new LetterTemplateService(
                DocumentTemplateSupport.templateEngine(messageSource, false), pdfRenderer));
    exporter =
        new LeaseAgreementExporter(
            contractRepository,
            propertyRepository,
            rentComponentRepository,
            rentPeriodRepository,
            new LeaseClauseResolver(templateRepository, overrideRepository, messageSource),
            new LeaseKindResolver(unitDetailsRepository),
            new LeaseDocumentLocator(),
            teamRepository,
            new LetterExporterHelper(
                contractPartyService, contactRepository, contactAddressRepository, unitRepository),
            templateService,
            messageSource,
            CLOCK);

    when(teamRepository.getById(TEAM_ID))
        .thenReturn(Team.builder().name("Verhuur Voorbeeld BV").build());
    when(propertyRepository.getByIdAndTeamId(PROPERTY_ID, TEAM_ID))
        .thenReturn(
            Property.builder()
                .id(PROPERTY_ID)
                .street("Keizersgracht 12")
                .postalCode("1015 CJ")
                .city("Amsterdam")
                .build());
    when(unitRepository.getByIdAndTeamId(UNIT_ID, TEAM_ID))
        .thenReturn(Unit.builder().id(UNIT_ID).unitNumber("2B").name(Optional.empty()).build());
    when(unitRepository.countActiveByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(1);
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(List.of());
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of());
    when(rentPeriodRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of());
  }

  // ---- fixtures -------------------------------------------------------------------------

  private record Spec(String key, boolean optional, boolean pinned) {}

  private static final List<Spec> NL_RESIDENTIAL =
      List.of(
          new Spec("parties", false, true),
          new Spec("premises", false, true),
          new Spec("term", false, false),
          new Spec("rent", false, false),
          new Spec("rent-adjustment", true, false),
          new Spec("service-costs", true, false),
          new Spec("deposit", true, false),
          new Spec("payment", false, false),
          new Spec("use", true, false),
          new Spec("subletting", true, false),
          new Spec("maintenance", true, false),
          new Spec("energy-label", false, false),
          new Spec("handover-inspection", true, false),
          new Spec("termination", false, false),
          new Spec("data-protection", true, false),
          new Spec("disputes", true, false));

  private static final List<Spec> LEGACY =
      List.of(
          new Spec("parties", false, false),
          new Spec("premises", false, false),
          new Spec("rent", false, false),
          new Spec("duration", false, false),
          new Spec("deposit", true, false),
          new Spec("maintenance", true, false),
          new Spec("termination-reference", true, false));

  private final Map<String, UUID> templateIds = new TreeMap<>();

  private List<LeaseClauseTemplate> nlTemplates() {
    List<LeaseClauseTemplate> out = new ArrayList<>();
    int order = 1;
    for (Spec s : NL_RESIDENTIAL) {
      out.add(template("NL", LeaseKind.RESIDENTIAL, s, order++, "lease.nl.residential."));
    }
    return out;
  }

  private List<LeaseClauseTemplate> legacyTemplates(String country) {
    List<LeaseClauseTemplate> out = new ArrayList<>();
    int order = 1;
    for (Spec s : LEGACY) {
      out.add(template(country, LeaseKind.LEGACY, s, order++, "lease."));
    }
    return out;
  }

  private LeaseClauseTemplate template(
      String country, LeaseKind kind, Spec s, int order, String keyPrefix) {
    UUID id = UUID.nameUUIDFromBytes((country + kind + s.key()).getBytes(UTF_8));
    templateIds.put(s.key(), id);
    boolean legacy = kind == LeaseKind.LEGACY;
    return LeaseClauseTemplate.builder()
        .id(id)
        .identifier(Optional.of(Sid.of(String.format("LCT%023d", order))))
        .countryCode(country)
        .leaseKind(kind)
        .clauseKey(s.key())
        .titleI18nKey(keyPrefix + s.key() + ".title")
        .bodyI18nKey(keyPrefix + s.key() + (legacy ? ".body" : ".summary"))
        .defaultIncluded(true)
        .optional(s.optional())
        .pinned(s.pinned())
        .sortOrder(order)
        .version(1)
        .build();
  }

  private static MoneyAmount eur(String v) {
    return new MoneyAmount(new BigDecimal(v), "EUR");
  }

  private Contract.ContractBuilder baseContract(String country) {
    return Contract.builder()
        .id(CONTRACT_ID)
        .teamId(TEAM_ID)
        .propertyId(PROPERTY_ID)
        .unitId(UNIT_ID)
        .identifier(Optional.of(Sid.of(CONTRACT_IDENTIFIER.value())))
        .countryCode(Optional.of(country))
        .contractType(Contract.ContractType.INDEFINITE)
        .startDate(LocalDate.of(2026, 2, 1))
        .rentAmount(eur("1250.00"))
        .paymentFrequency(Contract.PaymentFrequency.MONTHLY);
  }

  private void givenContract(Contract contract) {
    when(contractRepository.getByIdentifierAndTeamId(CONTRACT_IDENTIFIER, TEAM_ID))
        .thenReturn(contract);
  }

  private void givenTenants(String... nameAndEmail) {
    List<ContractParty> parties = new ArrayList<>();
    List<Contact> contacts = new ArrayList<>();
    for (int i = 0; i < nameAndEmail.length; i += 2) {
      UUID contactId = UUID.nameUUIDFromBytes(("contact" + i).getBytes(UTF_8));
      contacts.add(
          Contact.builder()
              .id(contactId)
              .displayName(nameAndEmail[i])
              .email(Optional.ofNullable(nameAndEmail[i + 1]))
              .build());
      parties.add(
          ContractParty.builder()
              .id(UUID.nameUUIDFromBytes(("party" + i).getBytes(UTF_8)))
              .contractId(CONTRACT_ID)
              .contactId(Optional.of(contactId))
              .role(i == 0 ? ContractPartyRole.PRIMARY_TENANT : ContractPartyRole.EXTRA_TENANT)
              .build());
    }
    when(contractPartyService.getPartiesForContract(CONTRACT_ID, TEAM_ID)).thenReturn(parties);
    when(contactRepository.findByIdsAndTeamId(any(), eq(TEAM_ID))).thenReturn(contacts);
  }

  private ContractRentComponent component(UUID periodId, RentComponentType type, String amount) {
    return ContractRentComponent.builder()
        .id(UUID.randomUUID())
        .contractId(CONTRACT_ID)
        .rentPeriodId(periodId)
        .componentType(type)
        .amount(eur(amount))
        .build();
  }

  private ContractLeaseClause override(String key, boolean included, int sortOrder) {
    return ContractLeaseClause.builder()
        .contractId(CONTRACT_ID)
        .clauseTemplateId(templateIds.get(key))
        .included(included)
        .sortOrder(sortOrder)
        .build();
  }

  // ---- scenarios ------------------------------------------------------------------------

  @Test
  @DisplayName("NL residential, de, indefinite, base + service costs, deposit via securityDeposit")
  void nlResidentialGermanIndefinite() throws IOException {
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(nlTemplates());
    UUID period = UUID.randomUUID();
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                component(period, RentComponentType.BASE_RENT, "1000.00"),
                component(period, RentComponentType.SERVICE_COSTS, "250.00")));
    givenContract(
        baseContract("NL")
            .rentAmount(eur("1250.00"))
            .securityDeposit(Optional.of(eur("2000.00")))
            .paymentDueDay(Optional.of(1))
            .build());
    givenTenants("J. Jansen", "jan@example.com", "P. de Vries", "piet@example.com");

    assertGolden("nl-residential-de-indefinite", "de", true);
  }

  @Test
  @DisplayName("NL residential, nl, fixed term, depositAmount wins, overrides, multi-unit address")
  void nlResidentialDutchFixedTerm() throws IOException {
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(nlTemplates());
    when(unitRepository.countActiveByPropertyIdAndTeamId(PROPERTY_ID, TEAM_ID)).thenReturn(3);
    UUID period = UUID.randomUUID();
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                component(period, RentComponentType.BASE_RENT, "900.00"),
                component(period, RentComponentType.UTILITIES_ADVANCE, "120.50"),
                component(period, RentComponentType.PARKING, "80.00")));
    givenContract(
        baseContract("NL")
            .contractType(Contract.ContractType.FIXED_TERM)
            .endDate(Optional.of(LocalDate.of(2028, 1, 31)))
            .depositAmount(Optional.of(eur("1800.00")))
            .securityDeposit(Optional.of(eur("999.00")))
            .paymentDueDay(Optional.of(25))
            .landlordNoticeDays(90)
            .tenantNoticeDays(60)
            .regionCode(Optional.of("NH"))
            .build());
    givenTenants("Maria Jansen", "maria@example.com");
    when(overrideRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                override("subletting", false, 10),
                override("deposit", true, 1),
                override("disputes", true, 2),
                override("parties", true, 99)));

    assertGolden("nl-residential-nl-fixedterm", "nl", true);
  }

  @Test
  @DisplayName("NL residential, en, two rent periods, duplicate tenant email, no region")
  void nlResidentialEnglishTwoRentPeriods() throws IOException {
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(nlTemplates());
    UUID oldPeriod = UUID.randomUUID();
    UUID currentPeriod = UUID.randomUUID();
    ContractRentPeriod old =
        ContractRentPeriod.builder()
            .id(oldPeriod)
            .effectiveFrom(LocalDate.of(2025, 1, 1))
            .effectiveTo(Optional.of(LocalDate.of(2025, 12, 31)))
            .build();
    ContractRentPeriod current =
        ContractRentPeriod.builder()
            .id(currentPeriod)
            .effectiveFrom(LocalDate.of(2026, 1, 1))
            .build();
    when(rentPeriodRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(old, current));
    when(rentComponentRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                component(oldPeriod, RentComponentType.BASE_RENT, "800.00"),
                component(currentPeriod, RentComponentType.BASE_RENT, "850.00"),
                component(currentPeriod, RentComponentType.SERVICE_COSTS, "40.00"),
                component(currentPeriod, RentComponentType.OTHER, "5.00")));
    givenContract(baseContract("NL").rentAmount(eur("895.00")).build());
    givenTenants(
        "A. Tenant", "Same@Example.com", "B. Tenant", "same@example.com", "C. Tenant", null);

    assertGolden("nl-residential-en-two-periods", "en", true);
  }

  @Test
  @DisplayName("BE legacy placeholder document (generic.html), no components, no deposit")
  void beLegacy() throws IOException {
    when(templateRepository.findByCountryAndKind("BE", LeaseKind.LEGACY))
        .thenReturn(legacyTemplates("BE"));
    givenContract(
        baseContract("BE")
            .contractType(Contract.ContractType.FIXED_TERM)
            .endDate(Optional.of(LocalDate.of(2027, 1, 31)))
            .build());
    givenTenants("L. Peeters", "lies@example.com");

    assertGolden("be-legacy-en", "en", false);
  }

  @Test
  @DisplayName("BE legacy in French with deposit via securityDeposit")
  void beLegacyFrench() throws IOException {
    when(templateRepository.findByCountryAndKind("BE", LeaseKind.LEGACY))
        .thenReturn(legacyTemplates("BE"));
    givenContract(baseContract("BE").securityDeposit(Optional.of(eur("1500.00"))).build());
    givenTenants("L. Peeters", "lies@example.com");

    assertGolden("be-legacy-fr", "fr", false);
  }

  // ---- harness --------------------------------------------------------------------------

  private void assertGolden(String name, String lang, boolean shell) throws IOException {
    LeaseAgreementExporter.RenderedLease rendered =
        exporter.generateWithLanguage(CONTRACT_IDENTIFIER, TEAM_ID, lang);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    if (shell) {
      verify(templateService)
          .renderToPdfTemplate(eq("lease-agreement/_shell"), any(Locale.class), captor.capture());
    } else {
      verify(templateService)
          .renderToPdf(eq("lease-agreement"), any(Locale.class), captor.capture());
    }
    String variables = canonical(captor.getValue(), 0);
    String html = normalise(renderedHtml.get());
    String languageUsed = "languageUsed=" + rendered.languageUsed() + "\n";

    compareOrWrite(name + ".variables.txt", languageUsed + variables);
    compareOrWrite(name + ".html.txt", html);
  }

  private static void compareOrWrite(String file, String actual) throws IOException {
    Path path = GOLDEN_DIR.resolve(file);
    if (System.getenv("LEASE_GOLDEN_WRITE") != null) {
      Files.createDirectories(GOLDEN_DIR);
      Files.writeString(path, actual, UTF_8);
      return;
    }
    String expected;
    try (InputStream in =
        LeaseRenderCharacterizationTest.class.getResourceAsStream(
            "/lease-characterization/" + file)) {
      assertThat(in)
          .as("golden file " + file + " (regenerate with LEASE_GOLDEN_WRITE=1)")
          .isNotNull();
      expected = new String(in.readAllBytes(), UTF_8);
    }
    assertThat(actual).as(file).isEqualTo(expected);
  }

  private static String normalise(String html) {
    return html.replaceAll("\\s+", " ").replaceAll("> <", ">\n<").strip() + "\n";
  }

  /** Deterministic rendering of the variable map: sorted keys, one entry per line. */
  private static String canonical(Object value, int depth) {
    String indent = "  ".repeat(depth);
    if (value instanceof Map<?, ?> map) {
      TreeMap<String, Object> sorted = new TreeMap<>();
      map.forEach((k, v) -> sorted.put(String.valueOf(k), v));
      return sorted.entrySet().stream()
          .map(
              e -> {
                Object v = e.getValue();
                boolean nested = v instanceof Map<?, ?> || v instanceof List<?>;
                return nested
                    ? indent + e.getKey() + ":\n" + canonical(v, depth + 1)
                    : indent + e.getKey() + "=" + describe(v) + "\n";
              })
          .collect(Collectors.joining());
    }
    if (value instanceof List<?> list) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < list.size(); i++) {
        Object v = list.get(i);
        if (v instanceof Map<?, ?> || v instanceof List<?>) {
          sb.append(indent).append("[").append(i).append("]:\n").append(canonical(v, depth + 1));
        } else {
          sb.append(indent).append("[").append(i).append("]=").append(describe(v)).append("\n");
        }
      }
      return sb.toString();
    }
    return indent + describe(value) + "\n";
  }

  private static String describe(Object v) {
    return v == null ? "<null>" : v.getClass().getSimpleName() + ":" + v;
  }
}
