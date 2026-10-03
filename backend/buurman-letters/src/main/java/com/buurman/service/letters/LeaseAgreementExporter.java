package com.buurman.service.letters;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    clauseResolver.templatesFor(contract, kind);
    Optional<LeaseDocumentLocator.LeaseDocument> document =
        LeaseClauseResolver.effectiveCountryCode(contract, property)
            .flatMap(cc -> documentLocator.locate(cc, kind, lang));
    String languageUsed =
        document.map(LeaseDocumentLocator.LeaseDocument::languageUsed).orElse(lang);
    Locale locale = LetterTemplateService.resolveLocale(languageUsed);

    List<ResolvedLeaseClauseResponse> includedClauses =
        clauseResolver.resolve(contract, locale, kind).stream()
            .filter(ResolvedLeaseClauseResponse::included)
            .toList();
    if (includedClauses.isEmpty()) {
      throw new BusinessRuleException(
          "No lease clauses are included for this contract — at least the required clauses must"
              + " remain included");
    }

    List<ContractRentComponent> rentComponents = activeRentComponents(contract, teamId);
    Map<String, Object> variables =
        buildTemplateVariables(contract, property, teamId, locale, rentComponents);
    if (document.isEmpty()) {
      variables.put("clauses", legacyClauses(includedClauses));
      return new RenderedLease(
          documentTemplateService.renderToPdf(DOCUMENT_TYPE, locale, variables), languageUsed);
    }

    includedClauses.forEach(
        c -> {
          if (!LeaseClauseTemplate.CLAUSE_KEY_PATTERN.matcher(c.clauseKey()).matches()) {
            throw new BusinessRuleException("Lease clause has an invalid key: " + c.clauseKey());
          }
        });
    LeaseDocumentLocator.LeaseDocument doc = document.get();
    requireClauseFragments(doc, includedClauses);
    addTypedValues(variables, contract, teamId, locale, rentComponents);
    variables.put("clauses", shellClauses(includedClauses));
    variables.put("clauseSource", doc.templatePath());
    variables.put("refs", clauseRefs(includedClauses));
    variables.put("authoritative", doc.authoritative());
    variables.put("languageUsed", languageUsed);
    variables.put("requestedLang", lang);
    variables.put("fallbackUsed", !languageUsed.equals(lang));
    return new RenderedLease(
        documentTemplateService.renderToPdfTemplate(SHELL_TEMPLATE, locale, variables),
        languageUsed);
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

  private Map<String, Object> buildTemplateVariables(
      Contract contract,
      Property property,
      UUID teamId,
      Locale locale,
      List<ContractRentComponent> components) {
    DateTimeFormatter dateFmt = LetterExporterHelper.letterDateFormatter(locale);
    Map<String, Object> vars =
        LetterExporterHelper.headerVariables(contract, LocalDate.now(clock), dateFmt);

    vars.putAll(helper.addressee(contract.getId(), teamId).variables());

    LetterExporterHelper.PremisesInfo premisesInfo =
        helper.premisesInfo(contract, property, messageSource, locale);
    vars.put("propertyAddress", LetterExporterHelper.premisesAddress(property, premisesInfo));

    vars.put(
        "rentComponents",
        components.stream()
            .map(
                c ->
                    Map.of(
                        "type",
                            messageSource.getMessage(
                                "lease.rentComponent." + c.getComponentType().name(), null, locale),
                        "amount",
                            CurrencyUtils.formatCurrency(
                                c.getAmount().value(), c.getAmount().currency(), locale)))
            .toList());

    vars.put(
        "signatureBlocks",
        helper.signatureBlocks(
            contract.getId(), teamId, messageSource, "letter.signature", locale));

    return vars;
  }

  /** Typed, pre-formatted values the per-language clause fragments may reference. */
  private void addTypedValues(
      Map<String, Object> vars,
      Contract contract,
      UUID teamId,
      Locale locale,
      List<ContractRentComponent> components) {
    DateTimeFormatter dateFmt = LetterExporterHelper.letterDateFormatter(locale);
    vars.put("landlordName", teamRepository.getById(teamId).getName());
    LetterExporterHelper.PartyData partyData = helper.loadPartyData(contract.getId(), teamId);
    vars.put(
        "tenantNames", helper.buildContactNamesList(partyData.parties(), partyData.contactMap()));
    vars.put("startDate", contract.getStartDate().format(dateFmt));
    vars.put("endDate", contract.getEndDate().map(d -> d.format(dateFmt)).orElse(null));
    // The lease regime follows the contract type; an end date alone does not make it fixed-term.
    vars.put("fixedTerm", contract.getContractType() == Contract.ContractType.FIXED_TERM);
    vars.put(
        "contractTypeLabel",
        messageSource.getMessage(
            "lease.contractType." + contract.getContractType().name(), null, locale));
    vars.put("rentAmount", formatMoney(baseRent(contract, components), locale));
    // Same precedence as the product's deposit card: depositAmount, else securityDeposit.
    vars.put(
        "depositAmount",
        contract
            .getDepositAmount()
            .or(contract::getSecurityDeposit)
            .map(m -> formatMoney(m, locale))
            .orElse(null));
    vars.put("paymentDueDay", contract.getPaymentDueDay().orElse(null));
    vars.put(
        "paymentFrequency",
        messageSource.getMessage(
            "lease.paymentFrequency." + contract.getPaymentFrequency().name(), null, locale));
    vars.put("landlordNoticeDays", contract.getLandlordNoticeDays());
    vars.put("tenantNoticeDays", contract.getTenantNoticeDays());
    vars.put("countryMetadata", contract.getCountryMetadata().orElse(null));
  }

  private static String formatMoney(MoneyAmount amount, Locale locale) {
    return CurrencyUtils.formatCurrency(amount.value(), amount.currency(), locale);
  }
}
