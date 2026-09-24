package com.buurman.service.letters;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.Contract;
import com.buurman.domain.Deposit;
import com.buurman.domain.DepositDeduction;
import com.buurman.domain.Property;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DepositRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.util.CurrencyUtils;

/**
 * Deposit statement: what was received, every deduction with its reason, what was returned and what
 * remains refundable. Given to the tenant on move-out; most jurisdictions require an itemised
 * statement within a statutory deadline.
 */
@Component
public class DepositStatementExporter {

  static final String DOCUMENT_TYPE = "deposit-statement";

  private final DepositRepository depositRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final LetterExporterHelper helper;
  private final LetterTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public DepositStatementExporter(
      DepositRepository depositRepository,
      ContractRepository contractRepository,
      PropertyRepository propertyRepository,
      LetterExporterHelper helper,
      LetterTemplateService documentTemplateService,
      @Qualifier("letterMessageSource") MessageSource messageSource,
      Clock clock) {
    this.depositRepository = depositRepository;
    this.contractRepository = contractRepository;
    this.propertyRepository = propertyRepository;
    this.helper = helper;
    this.documentTemplateService = documentTemplateService;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public byte[] generate(ContractIdentifier contractIdentifier, UUID teamId, String lang) {
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Deposit deposit =
        depositRepository
            .findByContractIdAndTeamId(contract.getId(), teamId)
            .orElseThrow(() -> new NotFoundException("No deposit recorded for this contract"));
    List<DepositDeduction> deductions = depositRepository.findDeductions(deposit.getId(), teamId);
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
    LetterExporterHelper.PartyData partyData = helper.loadPartyData(contract.getId(), teamId);
    Optional<Contact> primaryContact =
        helper.findPrimaryContact(partyData.parties(), partyData.contactMap());
    Optional<ContactAddress> address =
        primaryContact.flatMap(c -> helper.findMailingAddress(c.getId(), teamId));

    Locale locale = LetterTemplateService.resolveLocale(lang);
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
    String currency = deposit.getAmount().currency();
    BigDecimal deductionsTotal =
        deductions.stream()
            .map(d -> d.getAmount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refundable =
        deposit
            .getAmount()
            .value()
            .subtract(deductionsTotal)
            .subtract(deposit.getReturnedAmount())
            .max(BigDecimal.ZERO);

    Map<String, Object> vars = new HashMap<>();
    vars.put("generatedDate", LocalDate.now(clock).format(dateFmt));
    vars.put("depositIdentifier", deposit.getIdentifier().map(Object::toString).orElse(""));
    vars.put("contractIdentifier", contract.getIdentifier().map(Object::toString).orElse(""));
    vars.put("primaryContactName", primaryContact.map(Contact::getDisplayName).orElse(null));
    vars.put("contactAddress", helper.buildAddressMap(address).orElse(null));
    vars.put(
        "propertyAddress",
        property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity());
    vars.put("contractStart", contract.getStartDate().format(dateFmt));
    vars.put("contractEnd", contract.getEndDate().map(d -> d.format(dateFmt)).orElse(null));
    vars.put("status", deposit.getStatus().name());
    vars.put(
        "depositAmount",
        CurrencyUtils.formatCurrency(deposit.getAmount().value(), currency, locale));
    vars.put("receivedDate", deposit.getReceivedDate().map(d -> d.format(dateFmt)).orElse(null));
    vars.put("heldWhere", deposit.getHeldWhere().orElse(null));
    vars.put("returnDueDate", deposit.getReturnDueDate().map(d -> d.format(dateFmt)).orElse(null));
    vars.put(
        "deductions",
        deductions.stream()
            .map(
                d ->
                    Map.of(
                        "date", d.getDeductionDate().format(dateFmt),
                        "reason", d.getReason(),
                        "amount",
                            CurrencyUtils.formatCurrency(d.getAmount().value(), currency, locale)))
            .toList());
    vars.put("hasDeductions", !deductions.isEmpty());
    vars.put("deductionsTotal", CurrencyUtils.formatCurrency(deductionsTotal, currency, locale));
    vars.put("hasReturned", deposit.getReturnedAmount().signum() > 0);
    vars.put(
        "returnedAmount",
        CurrencyUtils.formatCurrency(deposit.getReturnedAmount(), currency, locale));
    vars.put("returnedDate", deposit.getReturnedDate().map(d -> d.format(dateFmt)).orElse(null));
    vars.put("refundable", CurrencyUtils.formatCurrency(refundable, currency, locale));
    vars.put(
        "isClosed",
        deposit.getStatus() == Deposit.DepositStatus.RETURNED
            || deposit.getStatus() == Deposit.DepositStatus.FORFEITED);
    vars.put("notes", deposit.getNotes().orElse(null));
    Optional<String> countryCode = contract.getCountryCode();
    vars.put("countryCode", countryCode.orElse(null));
    vars.put(
        "legalClause",
        helper
            .resolveLegalClause(messageSource, "deposit.legal.", countryCode, locale)
            .orElse(null));
    return documentTemplateService.renderToPdf(DOCUMENT_TYPE, locale, vars);
  }
}
