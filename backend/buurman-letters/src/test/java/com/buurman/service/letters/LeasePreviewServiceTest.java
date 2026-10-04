package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;

import com.buurman.document.DocumentRenderer;
import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.Contract;
import com.buurman.domain.LeaseAvailability;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.RentComponentType;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest.ClauseChoice;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest.Money;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest.RentComponent;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest.Sample;
import com.buurman.dto.response.backoffice.LeaseAgreementPreviewResponse;
import com.buurman.dto.response.backoffice.LeaseAgreementPreviewResponse.Clause;
import com.buurman.exception.BadRequestException;
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

@DisplayName("LeasePreviewService")
class LeasePreviewServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);
  private static final String CSP =
      "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; style-src"
          + " 'unsafe-inline'; img-src data:\">";

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

  private final LeaseClauseTemplateRepository templateRepository =
      mock(LeaseClauseTemplateRepository.class);
  private final DocumentRenderer pdfRenderer = mock(DocumentRenderer.class);
  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final PropertyRepository propertyRepository = mock(PropertyRepository.class);
  private final ContractRentComponentRepository rentComponentRepository =
      mock(ContractRentComponentRepository.class);
  private final TeamRepository teamRepository = mock(TeamRepository.class);
  private final UnitRepository unitRepository = mock(UnitRepository.class);

  private LeaseAgreementExporter exporter;
  private LeasePreviewService service;

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
    LeaseClauseResolver resolver =
        new LeaseClauseResolver(
            templateRepository, mock(ContractLeaseClauseRepository.class), messageSource);
    exporter =
        new LeaseAgreementExporter(
            contractRepository,
            propertyRepository,
            rentComponentRepository,
            mock(ContractRentPeriodRepository.class),
            resolver,
            new LeaseKindResolver(mock(UnitResidentialDetailsRepository.class)),
            new LeaseDocumentLocator(),
            teamRepository,
            new LetterExporterHelper(
                mock(ContractPartyService.class),
                mock(ContactRepository.class),
                mock(ContactAddressRepository.class),
                unitRepository),
            new LetterTemplateService(
                DocumentTemplateSupport.templateEngine(messageSource, false), pdfRenderer),
            messageSource,
            CLOCK);
    service = new LeasePreviewService(exporter, resolver, messageSource, CLOCK);

    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(
            templates("NL", LeaseKind.RESIDENTIAL, NL_RESIDENTIAL, "lease.nl.residential."));
    when(templateRepository.findByCountryAndKind("BE", LeaseKind.LEGACY))
        .thenReturn(templates("BE", LeaseKind.LEGACY, LEGACY, "lease."));
  }

  // ---- fixtures -------------------------------------------------------------------------

  private static List<LeaseClauseTemplate> templates(
      String country, LeaseKind kind, List<Spec> specs, String keyPrefix) {
    List<LeaseClauseTemplate> out = new ArrayList<>();
    int order = 1;
    for (Spec s : specs) {
      boolean legacy = kind == LeaseKind.LEGACY;
      out.add(
          LeaseClauseTemplate.builder()
              .id(
                  UUID.nameUUIDFromBytes(
                      (country + kind + s.key()).getBytes(StandardCharsets.UTF_8)))
              .identifier(Optional.of(Sid.of(String.format("LCT%023d", order))))
              .countryCode(country)
              .leaseKind(kind)
              .clauseKey(s.key())
              .titleI18nKey(keyPrefix + s.key() + ".title")
              .bodyI18nKey(keyPrefix + s.key() + (legacy ? ".body" : ".summary"))
              .defaultIncluded(true)
              .optional(s.optional())
              .pinned(s.pinned())
              .sortOrder(order++)
              .version(1)
              .build());
    }
    return out;
  }

  private static Sample sample() {
    return new Sample(
        Contract.ContractType.FIXED_TERM,
        LocalDate.of(2026, 3, 1),
        LocalDate.of(2027, 2, 28),
        List.of(
            new RentComponent(RentComponentType.BASE_RENT, new BigDecimal("1250.00"), "EUR"),
            new RentComponent(RentComponentType.SERVICE_COSTS, new BigDecimal("95.50"), "EUR")),
        new Money(new BigDecimal("2500.00"), "EUR"),
        1,
        Contract.PaymentFrequency.MONTHLY,
        "Verhuur Voorbeeld BV",
        List.of("Jan de Vries", "Anna Jansen"),
        "Keizersgracht 12, 1015 CJ Amsterdam",
        null);
  }

  private static LeaseAgreementPreviewRequest request(
      String country, LeaseKind kind, String lang, List<ClauseChoice> clauses) {
    return new LeaseAgreementPreviewRequest(country, kind, lang, sample(), clauses);
  }

  private static LeaseAgreementPreviewRequest nl() {
    return request("NL", LeaseKind.RESIDENTIAL, "nl", List.of());
  }

  private static LeaseAgreementPreviewRequest withSample(UnaryOperator<Sample> change) {
    return new LeaseAgreementPreviewRequest(
        "NL", LeaseKind.RESIDENTIAL, "nl", change.apply(sample()), List.of());
  }

  private static Sample copy(
      Sample s,
      String landlordName,
      List<String> tenants,
      String address,
      LocalDate start,
      LocalDate end,
      List<RentComponent> rent,
      Money deposit,
      Integer dueDay) {
    return new Sample(
        s.contractType(),
        start,
        end,
        rent,
        deposit,
        dueDay,
        s.paymentFrequency(),
        landlordName,
        tenants,
        address,
        s.regionCode());
  }

  private static Clause clause(LeaseAgreementPreviewResponse r, String key) {
    return r.clauses().stream().filter(c -> c.clauseKey().equals(key)).findFirst().orElseThrow();
  }

  // ---- validation -----------------------------------------------------------------------

  static Stream<Arguments> invalidRequests() {
    Sample s = sample();
    RentComponent rent = s.rentComponents().get(0);
    return Stream.of(
        Arguments.of("lowercase country", request("nl", LeaseKind.RESIDENTIAL, "nl", List.of())),
        Arguments.of("3-letter country", request("NLD", LeaseKind.RESIDENTIAL, "nl", List.of())),
        Arguments.of("digit country", request("N1", LeaseKind.RESIDENTIAL, "nl", List.of())),
        Arguments.of("null country", request(null, LeaseKind.RESIDENTIAL, "nl", List.of())),
        Arguments.of("unknown language", request("NL", LeaseKind.RESIDENTIAL, "xx", List.of())),
        Arguments.of("upper-case language", request("NL", LeaseKind.RESIDENTIAL, "NL", List.of())),
        Arguments.of("null language", request("NL", LeaseKind.RESIDENTIAL, null, List.of())),
        Arguments.of("null kind", request("NL", null, "nl", List.of())),
        Arguments.of(
            "null sample",
            new LeaseAgreementPreviewRequest("NL", LeaseKind.RESIDENTIAL, "nl", null, null)),
        Arguments.of(
            "clause key with underscore",
            request(
                "NL", LeaseKind.RESIDENTIAL, "nl", List.of(new ClauseChoice("Bad_Key", true, 1)))),
        Arguments.of(
            "clause key too long",
            request(
                "NL",
                LeaseKind.RESIDENTIAL,
                "nl",
                List.of(new ClauseChoice("a".repeat(65), true, 1)))),
        Arguments.of(
            "empty clause key",
            request("NL", LeaseKind.RESIDENTIAL, "nl", List.of(new ClauseChoice("", true, 1)))),
        Arguments.of(
            "null included",
            request("NL", LeaseKind.RESIDENTIAL, "nl", List.of(new ClauseChoice("rent", null, 1)))),
        Arguments.of(
            "null sortOrder",
            request(
                "NL", LeaseKind.RESIDENTIAL, "nl", List.of(new ClauseChoice("rent", true, null)))),
        Arguments.of(
            "fixed term without end date",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        null,
                        x.rentComponents(),
                        null,
                        1))),
        Arguments.of(
            "duplicate clause keys",
            request(
                "NL",
                LeaseKind.RESIDENTIAL,
                "nl",
                List.of(new ClauseChoice("rent", true, 1), new ClauseChoice("rent", false, 2)))),
        Arguments.of(
            "101 clauses",
            request(
                "NL",
                LeaseKind.RESIDENTIAL,
                "nl",
                IntStream.range(0, 101)
                    .mapToObj(i -> new ClauseChoice("c" + i, true, i))
                    .toList())),
        Arguments.of(
            "unknown clause key",
            request(
                "NL",
                LeaseKind.RESIDENTIAL,
                "nl",
                List.of(new ClauseChoice("no-such-clause", true, 1)))),
        Arguments.of(
            "no rent components",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        List.of(),
                        null,
                        1))),
        Arguments.of(
            "21 rent components",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        IntStream.range(0, 21).mapToObj(i -> rent).toList(),
                        null,
                        1))),
        Arguments.of(
            "11 tenants",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        IntStream.range(0, 11).mapToObj(i -> "T" + i).toList(),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        x.rentComponents(),
                        null,
                        1))),
        Arguments.of(
            "landlord name over 200",
            withSample(
                x ->
                    copy(
                        x,
                        "L".repeat(201),
                        List.of("T"),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        x.rentComponents(),
                        null,
                        1))),
        Arguments.of(
            "blank landlord name",
            withSample(
                x ->
                    copy(
                        x,
                        " ",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        x.rentComponents(),
                        null,
                        1))),
        Arguments.of(
            "tenant name over 200",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T".repeat(201)),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        x.rentComponents(),
                        null,
                        1))),
        Arguments.of(
            "address over 200",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A".repeat(201),
                        x.startDate(),
                        x.endDate(),
                        x.rentComponents(),
                        null,
                        1))),
        Arguments.of(
            "start date far in the past",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        LocalDate.of(1800, 1, 1),
                        null,
                        x.rentComponents(),
                        null,
                        1))),
        Arguments.of(
            "end before start",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        x.startDate().minusDays(1),
                        x.rentComponents(),
                        null,
                        1))),
        Arguments.of(
            "end far in the future",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        LocalDate.of(3000, 1, 1),
                        x.rentComponents(),
                        null,
                        1))),
        Arguments.of(
            "zero amount",
            rentOnly(new RentComponent(RentComponentType.BASE_RENT, BigDecimal.ZERO, "EUR"))),
        Arguments.of(
            "negative amount",
            rentOnly(new RentComponent(RentComponentType.BASE_RENT, new BigDecimal("-1"), "EUR"))),
        Arguments.of(
            "huge amount",
            rentOnly(
                new RentComponent(
                    RentComponentType.BASE_RENT, new BigDecimal("1000000000.01"), "EUR"))),
        Arguments.of(
            "exponent amount",
            rentOnly(
                new RentComponent(
                    RentComponentType.BASE_RENT, new BigDecimal("1E+10000000"), "EUR"))),
        Arguments.of(
            "lowercase currency",
            rentOnly(new RentComponent(RentComponentType.BASE_RENT, BigDecimal.TEN, "eur"))),
        Arguments.of(
            "unknown currency",
            rentOnly(new RentComponent(RentComponentType.BASE_RENT, BigDecimal.TEN, "ZZZ"))),
        Arguments.of(
            "null component type", rentOnly(new RentComponent(null, BigDecimal.TEN, "EUR"))),
        Arguments.of(
            "mixed currencies",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        List.of(
                            rent,
                            new RentComponent(RentComponentType.PARKING, BigDecimal.TEN, "USD")),
                        null,
                        1))),
        Arguments.of(
            "negative deposit",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        x.rentComponents(),
                        new Money(new BigDecimal("-5"), "EUR"),
                        1))),
        Arguments.of(
            "due day 0",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        x.rentComponents(),
                        null,
                        0))),
        Arguments.of(
            "due day 32",
            withSample(
                x ->
                    copy(
                        x,
                        "L",
                        List.of("T"),
                        "A",
                        x.startDate(),
                        x.endDate(),
                        x.rentComponents(),
                        null,
                        32))));
  }

  private static LeaseAgreementPreviewRequest rentOnly(RentComponent component) {
    return withSample(
        x ->
            copy(
                x,
                "L",
                List.of("T"),
                "A",
                x.startDate(),
                x.endDate(),
                List.of(component),
                null,
                1));
  }

  @ParameterizedTest(name = "rejects {0}")
  @MethodSource("invalidRequests")
  void rejectsInvalidRequestWithBadRequest(String name, LeaseAgreementPreviewRequest request) {
    assertThatThrownBy(() -> service.preview(request)).isInstanceOf(BadRequestException.class);
  }

  @Test
  @DisplayName("a null request is a bad request and nothing is looked up")
  void nullRequest() {
    assertThatThrownBy(() -> service.preview(null)).isInstanceOf(BadRequestException.class);
    verifyNoInteractions(templateRepository);
  }

  @Test
  @DisplayName("validation runs before any template lookup")
  void validationBeforeLookup() {
    assertThatThrownBy(() -> service.preview(request("nl", LeaseKind.RESIDENTIAL, "nl", List.of())))
        .isInstanceOf(BadRequestException.class);
    verifyNoInteractions(templateRepository);
  }

  @Test
  @DisplayName("LEGACY is an accepted kind: it is the placeholder example text")
  void legacyKindAccepted() {
    LeaseAgreementPreviewResponse r =
        service.preview(request("BE", LeaseKind.LEGACY, "en", List.of()));
    assertThat(r.availability()).isEqualTo(LeaseAvailability.AVAILABLE_EXAMPLE_TEXT);
    assertThat(r.kindUsed()).isEqualTo(LeaseKind.LEGACY);
  }

  @Test
  @DisplayName("no tenants and no optional values are accepted")
  void minimalSample() {
    LeaseAgreementPreviewRequest request =
        withSample(
            x ->
                new Sample(
                    Contract.ContractType.INDEFINITE,
                    x.startDate(),
                    null,
                    x.rentComponents(),
                    null,
                    null,
                    null,
                    "Landlord",
                    null,
                    "Somewhere 1",
                    null));
    assertThat(service.preview(request).html()).isNotBlank();
  }

  // ---- availability ---------------------------------------------------------------------

  @Test
  @DisplayName("NL residential renders the per-language document")
  void nlResidentialIsDocument() {
    LeaseAgreementPreviewResponse r = service.preview(nl());

    assertThat(r.availability()).isEqualTo(LeaseAvailability.AVAILABLE_DOCUMENT);
    assertThat(r.source()).isEqualTo(LeaseAgreementPreviewResponse.Source.DOCUMENT);
    assertThat(r.languageUsed()).isEqualTo("nl");
    assertThat(r.kindUsed()).isEqualTo(LeaseKind.RESIDENTIAL);
    assertThat(r.html())
        .contains("Verhuur Voorbeeld BV", "Jan de Vries", "Anna Jansen")
        .contains("1.250,00");
    assertThat(r.clauses()).hasSize(NL_RESIDENTIAL.size());
  }

  @Test
  @DisplayName("a furnished request falls back to the residential templates")
  void furnishedFallsBackToResidential() {
    LeaseAgreementPreviewResponse r =
        service.preview(request("NL", LeaseKind.RESIDENTIAL_FURNISHED, "nl", List.of()));

    assertThat(r.kindUsed()).isEqualTo(LeaseKind.RESIDENTIAL);
    assertThat(r.availability()).isEqualTo(LeaseAvailability.AVAILABLE_DOCUMENT);
  }

  @Test
  @DisplayName("BE has only the placeholder example text")
  void beIsExampleText() {
    LeaseAgreementPreviewResponse r =
        service.preview(request("BE", LeaseKind.RESIDENTIAL, "en", List.of()));

    assertThat(r.availability()).isEqualTo(LeaseAvailability.AVAILABLE_EXAMPLE_TEXT);
    assertThat(r.source()).isEqualTo(LeaseAgreementPreviewResponse.Source.EXAMPLE_TEXT);
    assertThat(r.kindUsed()).isEqualTo(LeaseKind.LEGACY);
    assertThat(r.html()).isNotBlank().contains("sample-banner");
  }

  @Test
  @DisplayName("an unavailable country is a 200 answer with no html")
  void unavailableCountry() {
    LeaseAgreementPreviewResponse r =
        service.preview(request("IT", LeaseKind.RESIDENTIAL, "it", List.of()));

    assertThat(r.availability()).isEqualTo(LeaseAvailability.UNAVAILABLE_COUNTRY);
    assertThat(r.html()).isNull();
    assertThat(r.languageUsed()).isNull();
    assertThat(r.kindUsed()).isNull();
    assertThat(r.source()).isNull();
    assertThat(r.clauses()).isEmpty();
  }

  @Test
  @DisplayName(
      "clause keys are not checked when the country is unavailable: nothing to check against")
  void unavailableCountryIgnoresClauseKeys() {
    LeaseAgreementPreviewResponse r =
        service.preview(
            request(
                "IT", LeaseKind.RESIDENTIAL, "it", List.of(new ClauseChoice("whatever", true, 1))));

    assertThat(r.availability()).isEqualTo(LeaseAvailability.UNAVAILABLE_COUNTRY);
  }

  // ---- overrides ------------------------------------------------------------------------

  @Test
  @DisplayName("a required clause stays included even when the request excludes it")
  void requiredCannotBeExcluded() {
    LeaseAgreementPreviewResponse r =
        service.preview(
            request(
                "NL",
                LeaseKind.RESIDENTIAL,
                "nl",
                List.of(new ClauseChoice("term", false, 1), new ClauseChoice("rent", false, 2))));

    assertThat(clause(r, "term").included()).isTrue();
    assertThat(clause(r, "term").optional()).isFalse();
    assertThat(clause(r, "term").articleNumber()).isPositive();
    assertThat(clause(r, "rent").included()).isTrue();
    assertThat(r.html()).contains(clause(r, "term").title());
  }

  @Test
  @DisplayName("a pinned clause keeps its template position whatever sortOrder is requested")
  void pinnedOrderIsFixed() {
    LeaseAgreementPreviewResponse r =
        service.preview(
            request(
                "NL",
                LeaseKind.RESIDENTIAL,
                "nl",
                List.of(
                    new ClauseChoice("parties", true, 999),
                    new ClauseChoice("disputes", true, -5))));

    assertThat(r.clauses().get(0).clauseKey()).isEqualTo("parties");
    assertThat(r.clauses().get(0).pinned()).isTrue();
    assertThat(r.clauses().get(0).articleNumber()).isEqualTo(1);
    assertThat(r.clauses().get(1).clauseKey()).isEqualTo("premises");
  }

  @Test
  @DisplayName("reordering optional clauses renumbers the articles")
  void reorderRenumbers() {
    LeaseAgreementPreviewResponse before = service.preview(nl());
    LeaseAgreementPreviewResponse after =
        service.preview(
            request(
                "NL", LeaseKind.RESIDENTIAL, "nl", List.of(new ClauseChoice("disputes", true, 0))));

    assertThat(clause(before, "disputes").articleNumber()).isEqualTo(NL_RESIDENTIAL.size());
    assertThat(clause(after, "disputes").articleNumber()).isEqualTo(3);
    assertThat(clause(after, "term").articleNumber())
        .isEqualTo(clause(before, "term").articleNumber() + 1);
    assertThat(after.html().indexOf(clause(after, "disputes").title()))
        .isLessThan(after.html().indexOf(clause(after, "term").title()));
  }

  @Test
  @DisplayName("an excluded optional clause is absent from the html but listed as excluded")
  void excludedOptionalAbsent() {
    LeaseAgreementPreviewResponse baseline = service.preview(nl());
    LeaseAgreementPreviewResponse r =
        service.preview(
            request(
                "NL",
                LeaseKind.RESIDENTIAL,
                "nl",
                List.of(new ClauseChoice("data-protection", false, 15))));

    Clause excluded = clause(r, "data-protection");
    assertThat(excluded.included()).isFalse();
    assertThat(excluded.articleNumber()).isZero();
    assertThat(baseline.html()).contains(excluded.title());
    assertThat(r.html()).doesNotContain(excluded.title());
  }

  // ---- sample banner + CSP --------------------------------------------------------------

  @Test
  @DisplayName("the preview html carries the SAMPLE banner, watermark and CSP")
  void bannerAndCspPresent() {
    String html = service.preview(nl()).html();

    assertThat(html)
        .contains(CSP)
        .contains("<div class=\"sample-banner\">SAMPLE &ndash; NOT A VALID LEASE</div>")
        .contains("content:\"SAMPLE\"");
    assertThat(html.indexOf(CSP)).isLessThan(html.indexOf("<style"));
    assertThat(html.indexOf("sample-banner\">")).isGreaterThan(html.indexOf("<body"));
    assertThat(html).contains("body::before");
  }

  @Test
  @DisplayName("the real PDF path hands the renderer html with neither banner, CSP nor watermark")
  void realRenderHasNeither() {
    UUID teamId = UUID.randomUUID();
    UUID propertyId = UUID.randomUUID();
    UUID unitId = UUID.randomUUID();
    ContractIdentifier identifier = ContractIdentifier.of("CON00000000000000000000009");
    Contract contract =
        Contract.builder()
            .id(UUID.randomUUID())
            .teamId(teamId)
            .propertyId(propertyId)
            .unitId(unitId)
            .identifier(Optional.of(Sid.of(identifier.value())))
            .countryCode(Optional.of("NL"))
            .contractType(Contract.ContractType.INDEFINITE)
            .startDate(LocalDate.of(2026, 2, 1))
            .rentAmount(new MoneyAmount(new BigDecimal("1250.00"), "EUR"))
            .paymentFrequency(Contract.PaymentFrequency.MONTHLY)
            .build();
    when(contractRepository.getByIdentifierAndTeamId(identifier, teamId)).thenReturn(contract);
    when(propertyRepository.getByIdAndTeamId(propertyId, teamId))
        .thenReturn(
            Property.builder()
                .id(propertyId)
                .street("Keizersgracht 12")
                .postalCode("1015 CJ")
                .city("Amsterdam")
                .build());
    when(unitRepository.getByIdAndTeamId(unitId, teamId))
        .thenReturn(Unit.builder().id(unitId).unitNumber("2B").name(Optional.empty()).build());
    when(teamRepository.getById(teamId))
        .thenReturn(Team.builder().name("Real Landlord BV").build());
    when(rentComponentRepository.findByContractIdAndTeamId(contract.getId(), teamId))
        .thenReturn(List.of());
    when(templateRepository.findByCountryAndKind("NL", LeaseKind.RESIDENTIAL))
        .thenReturn(
            templates("NL", LeaseKind.RESIDENTIAL, NL_RESIDENTIAL, "lease.nl.residential."));
    when(pdfRenderer.render(anyString(), any())).thenReturn(new byte[] {1});

    exporter.generateWithLanguage(identifier, teamId, "nl");

    ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
    verify(pdfRenderer).render(html.capture(), any());
    assertThat(html.getValue())
        .contains("Real Landlord BV")
        .doesNotContain("SAMPLE")
        .doesNotContain("sample-banner")
        .doesNotContain("Content-Security-Policy")
        .doesNotContain("body::before");
    assertThat(service.preview(nl()).html()).contains("sample-banner");
  }

  @Test
  @DisplayName("the shared templates and committed real-render goldens never mention the preview")
  void templatesAndGoldensAreClean() throws IOException {
    Path templates = Path.of("src/main/resources/templates/documents/lease-agreement");
    List<Path> files = new ArrayList<>();
    try (Stream<Path> s = Files.walk(templates)) {
      s.filter(p -> p.toString().endsWith(".html")).forEach(files::add);
    }
    try (Stream<Path> s = Files.walk(Path.of("src/test/resources/lease-characterization"))) {
      s.filter(Files::isRegularFile).forEach(files::add);
    }
    assertThat(files).isNotEmpty();
    for (Path file : files) {
      String content = Files.readString(file);
      assertThat(content)
          .as(file.toString())
          .doesNotContain("Content-Security-Policy")
          .doesNotContain("sample-banner");
    }
  }

  // ---- escaping -------------------------------------------------------------------------

  @Test
  @DisplayName("hostile sample strings are escaped and no script reaches the html")
  void hostileStringsEscaped() {
    LeaseAgreementPreviewRequest request =
        withSample(
            x ->
                copy(
                    x,
                    "<script>alert(1)</script>",
                    List.of("\"><img src=x onerror=alert(2)>", "<SCRIPT src=//evil></SCRIPT>"),
                    "<script>alert(3)</script> Street 1",
                    x.startDate(),
                    x.endDate(),
                    x.rentComponents(),
                    x.deposit(),
                    1));

    String html = service.preview(request).html();

    assertThat(html.toLowerCase()).doesNotContain("<script");
    assertThat(html).doesNotContain("<img src=x");
    assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
  }

  @Test
  @DisplayName("the legacy example-text path escapes hostile strings too")
  void legacyEscaped() {
    LeaseAgreementPreviewRequest request =
        new LeaseAgreementPreviewRequest(
            "BE",
            LeaseKind.RESIDENTIAL,
            "en",
            copySample(sample(), "<script>x</script>"),
            List.of());

    String html = service.preview(request).html();

    assertThat(html.toLowerCase()).doesNotContain("<script");
    assertThat(html).contains("&lt;script&gt;x&lt;/script&gt;");
  }

  private static Sample copySample(Sample s, String address) {
    return copy(
        s,
        s.landlordName(),
        s.tenantNames(),
        address,
        s.startDate(),
        s.endDate(),
        s.rentComponents(),
        s.deposit(),
        s.paymentDueDay());
  }
}
