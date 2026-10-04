package com.buurman.service.letters;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.Contract;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.RentComponentType;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest.ClauseChoice;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest.RentComponent;
import com.buurman.dto.request.backoffice.LeaseAgreementPreviewRequest.Sample;
import com.buurman.dto.response.backoffice.LeaseAgreementPreviewResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.service.LeaseClauseResolver;
import com.buurman.util.DocumentLanguages;
import com.buurman.util.MoneyAmount;

/**
 * Backoffice preview of a country lease agreement from synthetic sample values: the same clause
 * resolution (required / pinned / numbering) and the same shell or legacy template as a real lease,
 * rendered to HTML only. It never reads or writes a contract and never calls the PDF engine.
 *
 * <p>The {@code SAMPLE} banner, watermark and Content-Security-Policy are added here by
 * post-processing the rendered string, never in a shared template, so they cannot reach a real
 * lease. Every sample string goes through Thymeleaf {@code th:text} and is escaped.
 *
 * <p>{@code LEGACY} is an accepted kind: it selects the placeholder example text that countries
 * without a reviewed document render with.
 */
@Service
public class LeasePreviewService {

  static final int MAX_CLAUSES = 100;
  static final int MAX_RENT_COMPONENTS = 20;
  static final int MAX_TENANTS = 10;
  static final int MAX_TEXT_LENGTH = 200;
  static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");
  static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
  static final LocalDate MAX_DATE = LocalDate.of(2200, 1, 1);

  private static final String SAMPLE_REFERENCE = "SAMPLE";
  private static final int SAMPLE_NOTICE_DAYS = 30;
  private static final Pattern COUNTRY = Pattern.compile("[A-Z]{2}");
  private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");
  private static final Pattern REGION = Pattern.compile("[A-Za-z0-9-]{1,10}");

  private static final String CSP_META =
      "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; style-src"
          + " 'unsafe-inline'; img-src data:\">";
  private static final String SAMPLE_STYLE =
      "<style>"
          + ".sample-banner{position:fixed;top:0;left:0;right:0;z-index:10000;padding:6px 12px;"
          + "background:#b91c1c;color:#fff;font:700 12px/1.2 Helvetica,Arial,sans-serif;"
          + "letter-spacing:.08em;text-align:center}"
          + "body::before{content:\"SAMPLE\";position:fixed;top:38%;left:0;right:0;"
          + "text-align:center;font:700 160px/1 Helvetica,Arial,sans-serif;"
          + "color:rgba(185,28,28,.10);transform:rotate(-30deg);z-index:9999;"
          + "pointer-events:none}"
          + "body{padding-top:28px}"
          + "</style>";
  private static final String SAMPLE_BANNER =
      "<div class=\"sample-banner\">SAMPLE &ndash; NOT A VALID LEASE</div>";
  private static final Pattern HEAD_OPEN =
      Pattern.compile("<head(\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);
  private static final Pattern BODY_OPEN =
      Pattern.compile("<body(\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);

  private final LeaseAgreementExporter exporter;
  private final LeaseClauseResolver clauseResolver;
  private final MessageSource messageSource;
  private final Clock clock;

  public LeasePreviewService(
      LeaseAgreementExporter exporter,
      LeaseClauseResolver clauseResolver,
      @Qualifier("letterMessageSource") MessageSource messageSource,
      Clock clock) {
    this.exporter = exporter;
    this.clauseResolver = clauseResolver;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  /**
   * @throws BadRequestException for an invalid request or an unknown clause key
   * @throws com.buurman.exception.BusinessRuleException when no clause is included or a clause has
   *     no fragment in the document (rendered as a 409 ProblemDetail by the global handler)
   */
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public LeaseAgreementPreviewResponse preview(LeaseAgreementPreviewRequest request) {
    validate(request);
    Optional<String> country = Optional.of(request.countryCode());

    LeaseClauseResolver.Availability availability =
        clauseResolver.availabilityFor(country, request.leaseKind());
    if (!availability.state().isAvailable()) {
      return new LeaseAgreementPreviewResponse(
          availability.state(), null, null, null, null, List.of());
    }

    LeaseAgreementExporter.LeaseRenderPlan plan =
        exporter.plan(country, request.leaseKind(), request.language(), availability.templates());
    // Clause keys can only be checked against templates, so an unavailable country (no templates)
    // answers before this point and ignores them.
    List<LeaseClauseResolver.ClauseOverride> overrides =
        toOverrides(request.clauses(), plan.templates());
    LeaseAgreementExporter.AssembledLease assembled =
        exporter.assembleWithOverrides(
            plan, toRenderInput(request.sample(), plan.locale()), overrides);
    String html = markAsSample(exporter.renderHtml(assembled));

    return new LeaseAgreementPreviewResponse(
        availability.state(),
        html,
        plan.languageUsed(),
        plan.kindUsed(),
        plan.legacy()
            ? LeaseAgreementPreviewResponse.Source.EXAMPLE_TEXT
            : LeaseAgreementPreviewResponse.Source.DOCUMENT,
        assembled.clauses().stream()
            .map(
                c ->
                    new LeaseAgreementPreviewResponse.Clause(
                        c.clauseKey(),
                        c.title(),
                        c.included(),
                        c.optional(),
                        c.pinned(),
                        c.articleNumber(),
                        c.sortOrder()))
            .toList());
  }

  private static List<LeaseClauseResolver.ClauseOverride> toOverrides(
      List<ClauseChoice> choices, List<LeaseClauseTemplate> templates) {
    Map<String, UUID> idByKey =
        templates.stream()
            .collect(
                Collectors.toMap(LeaseClauseTemplate::getClauseKey, LeaseClauseTemplate::getId));
    return choices.stream()
        .map(
            c ->
                new LeaseClauseResolver.ClauseOverride(
                    Optional.ofNullable(idByKey.get(c.clauseKey()))
                        .orElseThrow(
                            () -> new BadRequestException("Unknown clause key: " + c.clauseKey())),
                    Boolean.TRUE.equals(c.included()),
                    Optional.ofNullable(c.sortOrder()).orElse(0)))
        .toList();
  }

  /** Synthetic render input from an already validated sample. */
  LeaseRenderInput toRenderInput(Sample sample, Locale locale) {
    List<LeaseRenderInput.RentLine> lines =
        sample.rentComponents().stream()
            .map(
                c ->
                    new LeaseRenderInput.RentLine(
                        c.type(), MoneyAmount.of(c.amount(), c.currency())))
            .toList();
    List<LeaseRenderInput.RentLine> base =
        lines.stream().filter(l -> l.type() == RentComponentType.BASE_RENT).toList();
    // Same rule as a real contract: the net rent is the BASE_RENT sum, else the entered rent.
    List<LeaseRenderInput.RentLine> baseSource = base.isEmpty() ? lines : base;
    MoneyAmount baseRent =
        MoneyAmount.of(
            baseSource.stream()
                .map(l -> l.amount().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add),
            baseSource.get(0).amount().currency());

    List<String> tenants = Optional.ofNullable(sample.tenantNames()).orElse(List.of());
    return new LeaseRenderInput(
        SAMPLE_REFERENCE,
        LocalDate.now(clock),
        Optional.ofNullable(sample.regionCode()),
        sample.startDate(),
        Optional.ofNullable(sample.endDate()),
        sample.contractType(),
        Optional.ofNullable(sample.paymentFrequency()).orElse(Contract.PaymentFrequency.MONTHLY),
        SAMPLE_NOTICE_DAYS,
        SAMPLE_NOTICE_DAYS,
        lines,
        baseRent,
        Optional.ofNullable(sample.deposit()).map(d -> MoneyAmount.of(d.amount(), d.currency())),
        Optional.ofNullable(sample.paymentDueDay()),
        Optional.empty(),
        Optional.of(sample.landlordName()),
        Optional.of(tenants.isEmpty() ? "—" : String.join(", ", tenants)),
        tenants.stream().findFirst(),
        Optional.empty(),
        sample.propertyAddress(),
        LetterExporterHelper.assembleSignatureBlocks(
            messageSource.getMessage("letter.signature", null, locale), tenants));
  }

  /**
   * Adds the Content-Security-Policy meta, the SAMPLE watermark style and the banner to rendered
   * HTML. Only this service calls it.
   */
  static String markAsSample(String html) {
    String withCsp = insertAfter(html, HEAD_OPEN, CSP_META + SAMPLE_STYLE);
    return insertAfter(withCsp, BODY_OPEN, SAMPLE_BANNER);
  }

  private static String insertAfter(String html, Pattern anchor, String insertion) {
    var matcher = anchor.matcher(html);
    if (!matcher.find()) {
      // Fail closed: a preview without its CSP or banner must never be returned, and prepending
      // before the doctype would leave the CSP meta outside <head>, where it is ignored.
      throw new IllegalStateException(
          "Lease preview template has no " + anchor.pattern() + " element");
    }
    return html.substring(0, matcher.end()) + insertion + html.substring(matcher.end());
  }

  // ---- validation -----------------------------------------------------------------------

  private static void validate(LeaseAgreementPreviewRequest request) {
    require(request != null, "Request body is required");
    require(
        request.countryCode() != null && COUNTRY.matcher(request.countryCode()).matches(),
        "countryCode must be a two-letter upper-case ISO code");
    require(
        request.language() != null && DocumentLanguages.isSupported(request.language()),
        "language must be one of " + DocumentLanguages.ORDERED);
    require(request.leaseKind() != null, "leaseKind is required");
    require(request.sample() != null, "sample is required");
    validateClauses(Optional.ofNullable(request.clauses()).orElse(List.of()));
    validateSample(request.sample());
  }

  private static void validateClauses(List<ClauseChoice> clauses) {
    require(clauses.size() <= MAX_CLAUSES, "At most " + MAX_CLAUSES + " clauses are allowed");
    Set<String> seen = new HashSet<>();
    for (ClauseChoice clause : clauses) {
      require(clause != null, "clauses must not contain null entries");
      require(
          clause.clauseKey() != null
              && LeaseClauseTemplate.CLAUSE_KEY_PATTERN.matcher(clause.clauseKey()).matches(),
          "clauseKey must match ^[a-z0-9-]{1,64}$");
      require(seen.add(clause.clauseKey()), "Duplicate clauseKey: " + clause.clauseKey());
      require(clause.included() != null, "clauses[].included is required");
      require(clause.sortOrder() != null, "clauses[].sortOrder is required");
    }
  }

  private static void validateSample(Sample sample) {
    require(sample.contractType() != null, "sample.contractType is required");
    requireText(sample.landlordName(), "sample.landlordName");
    requireText(sample.propertyAddress(), "sample.propertyAddress");
    List<String> tenants = Optional.ofNullable(sample.tenantNames()).orElse(List.of());
    require(tenants.size() <= MAX_TENANTS, "At most " + MAX_TENANTS + " tenants are allowed");
    tenants.forEach(t -> requireText(t, "sample.tenantNames entry"));

    require(sample.startDate() != null, "sample.startDate is required");
    requireSaneDate(sample.startDate(), "sample.startDate");
    require(
        sample.contractType() != Contract.ContractType.FIXED_TERM || sample.endDate() != null,
        "sample.endDate is required for a fixed-term contract");
    Optional.ofNullable(sample.endDate())
        .ifPresent(
            end -> {
              requireSaneDate(end, "sample.endDate");
              require(end.isAfter(sample.startDate()), "sample.endDate must be after startDate");
            });
    Optional.ofNullable(sample.paymentDueDay())
        .ifPresent(d -> require(d >= 1 && d <= 31, "sample.paymentDueDay must be 1..31"));
    Optional.ofNullable(sample.regionCode())
        .ifPresent(
            r ->
                require(
                    REGION.matcher(r).matches(),
                    "sample.regionCode must be 1-10 letters, digits or hyphens"));

    List<RentComponent> components = Optional.ofNullable(sample.rentComponents()).orElse(List.of());
    require(!components.isEmpty(), "At least one rent component is required");
    require(
        components.size() <= MAX_RENT_COMPONENTS,
        "At most " + MAX_RENT_COMPONENTS + " rent components are allowed");
    Set<String> currencies = new HashSet<>();
    for (RentComponent component : components) {
      require(component != null, "rentComponents must not contain null entries");
      require(component.type() != null, "rentComponent.type is required");
      requireAmount(component.amount(), component.currency());
      currencies.add(component.currency());
    }
    Optional.ofNullable(sample.deposit())
        .ifPresent(
            deposit -> {
              requireAmount(deposit.amount(), deposit.currency());
              currencies.add(deposit.currency());
            });
    require(currencies.size() == 1, "All amounts must use the same currency");
  }

  private static void requireAmount(BigDecimal amount, String currency) {
    require(amount != null, "amount is required");
    require(
        currency != null && CURRENCY.matcher(currency).matches() && isKnownCurrency(currency),
        "currency must be a known three-letter upper-case ISO 4217 code");
    // Throws BadRequestException for an out-of-range literal before any rescaling work.
    MoneyAmount.of(amount, currency);
    require(
        amount.signum() > 0 && amount.compareTo(MAX_AMOUNT) <= 0,
        "amount must be positive and at most " + MAX_AMOUNT.toPlainString());
  }

  private static boolean isKnownCurrency(String code) {
    try {
      return Currency.getInstance(code).getDefaultFractionDigits() >= 0;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static void requireText(String value, String field) {
    require(
        value != null && !value.isBlank() && value.length() <= MAX_TEXT_LENGTH,
        field + " must be 1-" + MAX_TEXT_LENGTH + " characters");
  }

  private static void requireSaneDate(LocalDate date, String field) {
    require(
        !date.isBefore(MIN_DATE) && date.isBefore(MAX_DATE),
        field + " must be between " + MIN_DATE + " and " + MAX_DATE);
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new BadRequestException(message);
    }
  }
}
