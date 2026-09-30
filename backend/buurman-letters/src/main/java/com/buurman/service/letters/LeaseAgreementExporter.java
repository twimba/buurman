package com.buurman.service.letters;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractRentComponent;
import com.buurman.domain.Property;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRentComponentRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.service.LeaseClauseResolver;
import com.buurman.util.CurrencyUtils;

/**
 * Full lease agreement PDF: the contract's resolved clauses rendered as the document body.
 *
 * <p>{@link LeaseClauseResolver} is the single source of truth for which clauses apply to a
 * contract and whether each is included — this exporter filters to the included subset before the
 * template ever sees them, so the template itself never has to re-check {@code clause.included}.
 * Every {@link #generate} call re-resolves clauses fresh; nothing here caches a prior resolution,
 * so a landlord who changes a clause selection and regenerates sees the new selection immediately.
 */
@Component
public class LeaseAgreementExporter {

  static final String DOCUMENT_TYPE = "lease-agreement";

  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRentComponentRepository rentComponentRepository;
  private final LeaseClauseResolver clauseResolver;
  private final LetterExporterHelper helper;
  private final LetterTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public LeaseAgreementExporter(
      ContractRepository contractRepository,
      PropertyRepository propertyRepository,
      ContractRentComponentRepository rentComponentRepository,
      LeaseClauseResolver clauseResolver,
      LetterExporterHelper helper,
      LetterTemplateService documentTemplateService,
      @Qualifier("letterMessageSource") MessageSource messageSource,
      Clock clock) {
    this.contractRepository = contractRepository;
    this.propertyRepository = propertyRepository;
    this.rentComponentRepository = rentComponentRepository;
    this.clauseResolver = clauseResolver;
    this.helper = helper;
    this.documentTemplateService = documentTemplateService;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public byte[] generate(ContractIdentifier contractIdentifier, UUID teamId, String lang) {
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
    Locale locale = LetterTemplateService.resolveLocale(lang);

    List<Map<String, String>> includedClauses = resolveIncludedClauses(contract, locale);
    if (includedClauses.isEmpty()) {
      throw new BusinessRuleException(
          "No lease clauses are included for this contract — at least the required clauses must"
              + " remain included");
    }

    Map<String, Object> variables =
        buildTemplateVariables(contract, property, teamId, includedClauses, locale);
    return documentTemplateService.renderToPdf(DOCUMENT_TYPE, locale, variables);
  }

  /**
   * Resolves every clause that applies to this contract, then keeps only the ones the resolver
   * marked included — the template that consumes the result never checks {@code included} itself.
   */
  private List<Map<String, String>> resolveIncludedClauses(Contract contract, Locale locale) {
    return clauseResolver.resolve(contract, locale).stream()
        .filter(ResolvedLeaseClauseResponse::included)
        .map(c -> Map.of("title", c.title(), "body", c.body()))
        .toList();
  }

  private Map<String, Object> buildTemplateVariables(
      Contract contract,
      Property property,
      UUID teamId,
      List<Map<String, String>> includedClauses,
      Locale locale) {
    DateTimeFormatter dateFmt = LetterExporterHelper.letterDateFormatter(locale);
    Map<String, Object> vars =
        LetterExporterHelper.headerVariables(contract, LocalDate.now(clock), dateFmt);

    vars.putAll(helper.addressee(contract.getId(), teamId).variables());

    LetterExporterHelper.PremisesInfo premisesInfo =
        helper.premisesInfo(contract, property, messageSource, locale);
    vars.put("propertyAddress", LetterExporterHelper.premisesAddress(property, premisesInfo));

    vars.put("clauses", includedClauses);

    List<ContractRentComponent> components =
        rentComponentRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    vars.put(
        "rentComponents",
        components.stream()
            .map(
                c ->
                    Map.of(
                        "type", c.getComponentType().getDisplayName(),
                        "amount",
                            CurrencyUtils.formatCurrency(
                                c.getAmount().value(), c.getAmount().currency(), locale)))
            .toList());

    return vars;
  }
}
