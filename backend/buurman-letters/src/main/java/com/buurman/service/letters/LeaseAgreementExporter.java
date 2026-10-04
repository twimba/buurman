package com.buurman.service.letters;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractRentComponent;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Property;
import com.buurman.domain.RentComponentType;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRentComponentRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.service.LeaseClauseResolver;
import com.buurman.service.LeaseKindResolver;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

/**
 * Full lease agreement PDF: the contract's resolved clauses rendered as the document body.
 *
 * <p>{@link LeaseClauseResolver} is the single source of truth for which clauses apply to a
 * contract and whether each is included — this exporter filters to the included subset before the
 * template ever sees them, so the template itself never has to re-check {@code clause.included}.
 * Every {@link #generate} call re-resolves clauses fresh; nothing here caches a prior resolution,
 * so a landlord who changes a clause selection and regenerates sees the new selection immediately.
 *
 * <p>Assumption: the {@code landlordName} template value is the team's name, a stand-in until the
 * contract carries an explicit landlord party; callers should confirm that is acceptable.
 */
@Component
public class LeaseAgreementExporter {

  static final String DOCUMENT_TYPE = "lease-agreement";
  static final String SHELL_TEMPLATE = "lease-agreement/_shell";

  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRentComponentRepository rentComponentRepository;
  private final ContractRentPeriodRepository rentPeriodRepository;
  private final LeaseClauseResolver clauseResolver;
  private final LeaseKindResolver leaseKindResolver;
  private final LeaseDocumentLocator documentLocator;
  private final TeamRepository teamRepository;
  private final LetterExporterHelper helper;
  private final LetterTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public LeaseAgreementExporter(
      ContractRepository contractRepository,
      PropertyRepository propertyRepository,
      ContractRentComponentRepository rentComponentRepository,
      ContractRentPeriodRepository rentPeriodRepository,
      LeaseClauseResolver clauseResolver,
      LeaseKindResolver leaseKindResolver,
      LeaseDocumentLocator documentLocator,
      TeamRepository teamRepository,
      LetterExporterHelper helper,
      LetterTemplateService documentTemplateService,
      @Qualifier("letterMessageSource") MessageSource messageSource,
      Clock clock) {
    this.contractRepository = contractRepository;
    this.propertyRepository = propertyRepository;
    this.rentComponentRepository = rentComponentRepository;
    this.rentPeriodRepository = rentPeriodRepository;
    this.clauseResolver = clauseResolver;
    this.leaseKindResolver = leaseKindResolver;
    this.documentLocator = documentLocator;
    this.teamRepository = teamRepository;
    this.helper = helper;
    this.documentTemplateService = documentTemplateService;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  /** The rendered PDF together with the language it was actually rendered in. */
  public record RenderedLease(byte[] pdf, String languageUsed) {}

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public byte[] generate(ContractIdentifier contractIdentifier, UUID teamId, String lang) {
    return generateWithLanguage(contractIdentifier, teamId, lang).pdf();
  }

  /**
   * Renders the agreement. When a per-language document exists but only in another language than
   * requested, the whole PDF (chrome, clause titles, notices, formats) is rendered in that language
   * so the output is never mixed-language, and a fallback notice names the requested one.
   */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public RenderedLease generateWithLanguage(
      ContractIdentifier contractIdentifier, UUID teamId, String lang) {
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);

    LeaseKind kind = leaseKindResolver.resolveFor(contract, property, teamId);
    // Fail fast for an unavailable country before any document lookup or rendering work.
    Optional<String> country = LeaseClauseResolver.effectiveCountryCode(contract, property);
    LeaseRenderPlan plan = plan(country, kind, lang);

    List<ResolvedLeaseClauseResponse> includedClauses =
        requireIncluded(clauseResolver.resolve(contract, country, plan.locale(), plan.templates()));
    // Before any repository access, so a broken document fails with the same error as always.
    validateShell(plan, includedClauses);

    LeaseRenderInput input = loadInput(contract, property, teamId, plan);
    return render(assembleResolved(plan, input, includedClauses, includedClauses));
  }

  /** Which templates and per-language document apply, and the language the output is in. */
  record LeaseRenderPlan(
      Optional<String> country,
      LeaseKind kind,
      String requestedLang,
      List<LeaseClauseTemplate> templates,
      Optional<LeaseDocumentLocator.LeaseDocument> document,
      String languageUsed,
      Locale locale) {

    /** The legacy {@code generic.html} path applies when no per-language document exists. */
    boolean legacy() {
      return document.isEmpty();
    }

    /** The kind whose templates apply: the first of the fallback chain that has any. */
    LeaseKind kindUsed() {
      return templates.get(0).getLeaseKind();
    }
  }

  /**
   * The template and variables ready to hand to {@link LetterTemplateService}.
   *
   * @param clauses every resolved clause (article number, forced-included, pinned, excluded ones
   *     too) when assembled from overrides; the included subset when assembled from a real
   *     contract's already-filtered list
   */
  record AssembledLease(
      boolean legacy,
      String templateName,
      Locale locale,
      Map<String, Object> variables,
      String languageUsed,
      List<ResolvedLeaseClauseResponse> clauses) {}

  /**
   * Looks up templates (failing fast for an unavailable country) and locates the per-language
   * document. Holds no contract state, so a synthetic caller can use it with a request's country.
   */
  LeaseRenderPlan plan(Optional<String> country, LeaseKind kind, String lang) {
    return plan(country, kind, lang, clauseResolver.templatesFor(country, kind));
  }

  /** As {@link #plan(Optional, LeaseKind, String)} for templates the caller already fetched. */
  LeaseRenderPlan plan(
      Optional<String> country, LeaseKind kind, String lang, List<LeaseClauseTemplate> templates) {
    Optional<LeaseDocumentLocator.LeaseDocument> document =
        country.flatMap(cc -> documentLocator.locate(cc, kind, lang));
    String languageUsed =
        document.map(LeaseDocumentLocator.LeaseDocument::languageUsed).orElse(lang);
    return new LeaseRenderPlan(
        country,
        kind,
        lang,
        templates,
        document,
        languageUsed,
        LetterTemplateService.resolveLocale(languageUsed));
  }

  /** Keeps the included clauses; a document with none cannot be rendered. */
  static List<ResolvedLeaseClauseResponse> requireIncluded(
      List<ResolvedLeaseClauseResponse> resolved) {
    List<ResolvedLeaseClauseResponse> included =
        resolved.stream().filter(ResolvedLeaseClauseResponse::included).toList();
    if (included.isEmpty()) {
      throw new BusinessRuleException(
          "No lease clauses are included for this contract — at least the required clauses must"
              + " remain included");
    }
    return included;
  }

  /**
   * Resolves the plan's templates against explicit overrides (same required/pinned/numbering rules
   * as for a real contract) and assembles the document. The result's {@code clauses} is the full
   * resolved list, including excluded optional clauses.
   */
  AssembledLease assembleWithOverrides(
      LeaseRenderPlan plan,
      LeaseRenderInput input,
      Collection<LeaseClauseResolver.ClauseOverride> overrides) {
    List<ResolvedLeaseClauseResponse> resolved =
        clauseResolver.resolve(plan.templates(), overrides, plan.country(), plan.locale());
    List<ResolvedLeaseClauseResponse> included = requireIncluded(resolved);
    validateShell(plan, included);
    return assembleResolved(plan, input, included, resolved);
  }

  /**
   * A shell document needs a valid key and a fragment for every included clause; checking up front
   * names the clause instead of failing deep inside the render. The legacy path has no fragments.
   */
  static void validateShell(LeaseRenderPlan plan, List<ResolvedLeaseClauseResponse> included) {
    plan.document()
        .ifPresent(
            doc -> {
              included.forEach(
                  c -> {
                    if (!LeaseClauseTemplate.CLAUSE_KEY_PATTERN.matcher(c.clauseKey()).matches()) {
                      throw new BusinessRuleException(
                          "Lease clause has an invalid key: " + c.clauseKey());
                    }
                  });
              requireClauseFragments(doc, included);
            });
  }

  /**
   * Builds the template name and variables from the plan, input and included clauses; {@code
   * allClauses} is only passed through to the result.
   */
  AssembledLease assembleResolved(
      LeaseRenderPlan plan,
      LeaseRenderInput input,
      List<ResolvedLeaseClauseResponse> includedClauses,
      List<ResolvedLeaseClauseResponse> allClauses) {
    Map<String, Object> variables = variables(input, plan.locale());
    if (plan.legacy()) {
      variables.put("clauses", legacyClauses(includedClauses));
      return new AssembledLease(
          true, DOCUMENT_TYPE, plan.locale(), variables, plan.languageUsed(), allClauses);
    }

    LeaseDocumentLocator.LeaseDocument doc = plan.document().orElseThrow();
    addTypedValues(
        variables,
        input,
        plan.locale(),
        plan.country()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "country is required for a per-language lease document")));
    variables.put("clauses", shellClauses(includedClauses));
    variables.put("clauseSource", doc.templatePath());
    variables.put("refs", clauseRefs(includedClauses));
    variables.put("authoritative", doc.authoritative());
    variables.put("noNationalVersion", doc.noNationalVersion());
    variables.put("languageUsed", plan.languageUsed());
    variables.put("requestedLang", plan.requestedLang());
    variables.put("fallbackUsed", !plan.languageUsed().equals(plan.requestedLang()));
    return new AssembledLease(
        false, SHELL_TEMPLATE, plan.locale(), variables, plan.languageUsed(), allClauses);
  }

  /**
   * Renders the assembled lease to HTML without the PDF engine, with the same template choice as
   * {@link #render}. Used by the backoffice preview.
   */
  String renderHtml(AssembledLease lease) {
    return lease.legacy()
        ? documentTemplateService.renderToHtml(
            lease.templateName(), lease.locale(), lease.variables())
        : documentTemplateService.renderTemplateToHtml(
            lease.templateName(), lease.locale(), lease.variables());
  }

  /** The real path's final step: the same {@code LetterTemplateService} calls as always. */
  private RenderedLease render(AssembledLease lease) {
    byte[] pdf =
        lease.legacy()
            ? documentTemplateService.renderToPdf(
                lease.templateName(), lease.locale(), lease.variables())
            : documentTemplateService.renderToPdfTemplate(
                lease.templateName(), lease.locale(), lease.variables());
    return new RenderedLease(pdf, lease.languageUsed());
  }

  /**
   * Everything the render needs from the database, resolved for one real contract. The typed
   * landlord/tenant values are only loaded when the per-language document path will use them.
   */
  LeaseRenderInput loadInput(
      Contract contract, Property property, UUID teamId, LeaseRenderPlan plan) {
    Locale locale = plan.locale();
    List<ContractRentComponent> components = activeRentComponents(contract, teamId);

    LetterExporterHelper.Addressee addressee = helper.addressee(contract.getId(), teamId);
    LetterExporterHelper.PremisesInfo premisesInfo =
        helper.premisesInfo(contract, property, messageSource, locale);
    List<Map<String, String>> signatureBlocks =
        helper.signatureBlocks(contract.getId(), teamId, messageSource, "letter.signature", locale);

    Optional<String> landlordName = Optional.empty();
    Optional<String> tenantNames = Optional.empty();
    if (!plan.legacy()) {
      landlordName = Optional.of(teamRepository.getById(teamId).getName());
      LetterExporterHelper.PartyData partyData = helper.loadPartyData(contract.getId(), teamId);
      tenantNames =
          Optional.of(helper.buildContactNamesList(partyData.parties(), partyData.contactMap()));
    }

    return new LeaseRenderInput(
        contract
            .getIdentifier()
            .orElseThrow(() -> new IllegalStateException("Contract missing identifier"))
            .value(),
        LocalDate.now(clock),
        contract.getRegionCode(),
        contract.getStartDate(),
        contract.getEndDate(),
        contract.getContractType(),
        contract.getPaymentFrequency(),
        contract.getLandlordNoticeDays(),
        contract.getTenantNoticeDays(),
        components.stream()
            .map(c -> new LeaseRenderInput.RentLine(c.getComponentType(), c.getAmount()))
            .toList(),
        baseRent(contract, components),
        contract.getDepositAmount().or(contract::getSecurityDeposit),
        contract.getPaymentDueDay(),
        contract.getCountryMetadata(),
        landlordName,
        tenantNames,
        addressee.contact().map(Contact::getDisplayName),
        LetterExporterHelper.buildAddressMap(addressee.address()),
        LetterExporterHelper.premisesAddress(property, premisesInfo),
        signatureBlocks);
  }

  private static List<Map<String, String>> legacyClauses(
      List<ResolvedLeaseClauseResponse> included) {
    return included.stream().map(c -> Map.of("title", c.title(), "body", c.body())).toList();
  }

  /** Plain maps, not records: keeps SpEL property access in the templates trivially safe. */
  private static List<Map<String, Object>> shellClauses(
      List<ResolvedLeaseClauseResponse> included) {
    return included.stream()
        .map(
            c -> {
              Map<String, Object> m = new HashMap<>();
              m.put("clauseKey", c.clauseKey());
              m.put("title", c.title());
              m.put("articleNumber", c.articleNumber());
              m.put("pinned", c.pinned());
              m.put("optional", c.optional());
              return m;
            })
        .toList();
  }

  private static Map<String, Integer> clauseRefs(List<ResolvedLeaseClauseResponse> included) {
    Map<String, Integer> refs = new HashMap<>();
    included.forEach(c -> refs.put(c.clauseKey(), c.articleNumber()));
    return refs;
  }

  /**
   * A missing fragment makes Thymeleaf fail deep inside the PDF render (see {@link
   * LeaseDocumentFragments}); checking up front names the clause key and the document instead.
   */
  private static void requireClauseFragments(
      LeaseDocumentLocator.LeaseDocument doc, List<ResolvedLeaseClauseResponse> included) {
    Set<String> fragments = LeaseDocumentFragments.names(doc.templatePath());
    included.forEach(
        c -> {
          if (!fragments.contains("clause-" + c.clauseKey())) {
            throw new BusinessRuleException(
                "Lease document "
                    + doc.templatePath()
                    + " has no fragment for clause '"
                    + c.clauseKey()
                    + "'");
          }
        });
  }

  /**
   * The contract's rent components of the rent period that applies now (else the first one):
   * components exist per rent period, so mixing periods would double-count the rent.
   */
  private List<ContractRentComponent> activeRentComponents(Contract contract, UUID teamId) {
    List<ContractRentComponent> all =
        rentComponentRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    long periods = all.stream().map(ContractRentComponent::getRentPeriodId).distinct().count();
    if (periods <= 1) {
      return all;
    }
    LocalDate today = LocalDate.now(clock);
    List<ContractRentPeriod> candidates =
        rentPeriodRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    Optional<UUID> activePeriodId =
        candidates.stream()
            .filter(p -> !p.getEffectiveFrom().isAfter(today))
            .filter(p -> p.getEffectiveTo().map(to -> !to.isBefore(today)).orElse(true))
            .max(Comparator.comparing(ContractRentPeriod::getEffectiveFrom))
            .or(
                () ->
                    candidates.stream()
                        .min(Comparator.comparing(ContractRentPeriod::getEffectiveFrom)))
            .map(ContractRentPeriod::getId);
    return activePeriodId
        .map(
            id ->
                all.stream()
                    .filter(c -> id.equals(c.getRentPeriodId()))
                    .collect(Collectors.toList()))
        .orElse(all);
  }

  /**
   * The net rent ("kale huur"): {@code Contract.rentAmount} is the SUM of all rent components (base
   * rent, service costs, parking, ...) whenever components were supplied on create/update, so the
   * base rent is the sum of the BASE_RENT components. Contracts without components, or whose
   * components include no BASE_RENT, fall back to {@code Contract.rentAmount} (for a component-less
   * contract that is the entered rent itself).
   */
  static MoneyAmount baseRent(Contract contract, List<ContractRentComponent> components) {
    List<ContractRentComponent> base =
        components.stream()
            .filter(c -> c.getComponentType() == RentComponentType.BASE_RENT)
            .toList();
    if (base.isEmpty()) {
      return contract.getRentAmount();
    }
    BigDecimal sum =
        base.stream().map(c -> c.getAmount().value()).reduce(BigDecimal.ZERO, BigDecimal::add);
    return MoneyAmount.of(sum, base.get(0).getAmount().currency());
  }

  /**
   * The template variable map every path receives. The pre-formatted values the per-language clause
   * fragments reference are added by {@link #addTypedValues}; the legacy {@code generic.html} path
   * must not receive them.
   */
  Map<String, Object> variables(LeaseRenderInput input, Locale locale) {
    DateTimeFormatter dateFmt = LetterExporterHelper.letterDateFormatter(locale);
    Map<String, Object> vars =
        LetterExporterHelper.headerVariables(input.contractIdentifier(), input.today(), dateFmt);
    vars.putAll(
        LetterExporterHelper.addresseeVariables(
            input.primaryContactName(), input.contactAddress()));
    vars.put("propertyAddress", input.propertyAddress());

    vars.put(
        "rentComponents",
        input.rentComponents().stream()
            .map(
                c ->
                    Map.of(
                        "type",
                            messageSource.getMessage(
                                "lease.rentComponent." + c.type().name(), null, locale),
                        "amount", formatMoney(c.amount(), locale)))
            .toList());
    vars.put("signatureBlocks", input.signatureBlocks());
    return vars;
  }

  /**
   * Typed, pre-formatted values the per-language clause fragments may reference; {@code
   * countryCode} is the effective country the document was located for.
   */
  private void addTypedValues(
      Map<String, Object> vars, LeaseRenderInput input, Locale locale, String countryCode) {
    DateTimeFormatter dateFmt = LetterExporterHelper.letterDateFormatter(locale);
    vars.put(
        "landlordName",
        input
            .landlordName()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "landlordName is required for a per-language lease document")));
    vars.put(
        "tenantNames",
        input
            .tenantNames()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "tenantNames is required for a per-language lease document")));
    vars.put("startDate", input.startDate().format(dateFmt));
    vars.put("endDate", input.endDate().map(d -> d.format(dateFmt)).orElse(null));
    // The lease regime follows the contract type; an end date alone does not make it fixed-term.
    vars.put("fixedTerm", input.contractType() == Contract.ContractType.FIXED_TERM);
    vars.put(
        "contractTypeLabel",
        messageSource.getMessage(
            "lease.contractType." + input.contractType().name(), null, locale));
    vars.put("rentAmount", formatMoney(input.baseRent(), locale));
    vars.put("depositAmount", input.deposit().map(m -> formatMoney(m, locale)).orElse(null));
    vars.put("paymentDueDay", input.paymentDueDay().orElse(null));
    vars.put(
        "paymentFrequency",
        messageSource.getMessage(
            "lease.paymentFrequency." + input.paymentFrequency().name(), null, locale));
    vars.put("landlordNoticeDays", input.landlordNoticeDays());
    vars.put("tenantNoticeDays", input.tenantNoticeDays());
    vars.put("countryMetadata", input.countryMetadata().orElse(null));
    // Lets a document branch on nation and region/state ({@code th:if="${regionCode == 'X'}"});
    // the region is null when the contract has none.
    vars.put("regionCode", input.regionCode().orElse(null));
    vars.put("countryCode", countryCode);
  }

  private static String formatMoney(MoneyAmount amount, Locale locale) {
    return CurrencyUtils.formatCurrency(amount.value(), amount.currency(), locale);
  }
}
