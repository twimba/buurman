package com.buurman.service.letters;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
import com.buurman.domain.ContractPaymentInstruction;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentInstruction;
import com.buurman.domain.Property;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPaymentInstructionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.document.TenantNoticeDocumentService;
import com.buurman.util.CurrencyUtils;

/**
 * Formal notice of overdue rent (ingebrekestelling / Mahnung / mise en demeure): a dated letter to
 * the tenant stating the outstanding amount, a payment deadline and the payment details. Sent with
 * FINAL-tone reminders and downloadable from the payment.
 */
@Component
public class PaymentFormalNoticeExporter implements TenantNoticeDocumentService {

  static final String DOCUMENT_TYPE = "payment-formal-notice";

  /** Fallback when neither the contract nor the country regulation sets a notice period. */
  static final int DEFAULT_DEADLINE_DAYS = 14;

  private final PaymentRepository paymentRepository;
  private final PaymentReceivalRepository receivalRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final ContactRepository contactRepository;
  private final ContractPartyService contractPartyService;
  private final ContractPaymentInstructionRepository cpiRepository;
  private final PaymentInstructionRepository paymentInstructionRepository;
  private final RentRegulationRepository rentRegulationRepository;
  private final LetterExporterHelper helper;
  private final LetterTemplateService documentTemplateService;
  private final MessageSource messageSource;
  private final Clock clock;

  public PaymentFormalNoticeExporter(
      PaymentRepository paymentRepository,
      PaymentReceivalRepository receivalRepository,
      ContractRepository contractRepository,
      PropertyRepository propertyRepository,
      ContactRepository contactRepository,
      ContractPartyService contractPartyService,
      ContractPaymentInstructionRepository cpiRepository,
      PaymentInstructionRepository paymentInstructionRepository,
      RentRegulationRepository rentRegulationRepository,
      LetterExporterHelper helper,
      LetterTemplateService documentTemplateService,
      @Qualifier("letterMessageSource") MessageSource messageSource,
      Clock clock) {
    this.paymentRepository = paymentRepository;
    this.receivalRepository = receivalRepository;
    this.contractRepository = contractRepository;
    this.propertyRepository = propertyRepository;
    this.contactRepository = contactRepository;
    this.contractPartyService = contractPartyService;
    this.cpiRepository = cpiRepository;
    this.paymentInstructionRepository = paymentInstructionRepository;
    this.rentRegulationRepository = rentRegulationRepository;
    this.helper = helper;
    this.documentTemplateService = documentTemplateService;
    this.messageSource = messageSource;
    this.clock = clock;
  }

  /** On-demand download for a payment, resolving tenant and balance from the payment itself. */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public byte[] generate(PaymentIdentifier paymentIdentifier, UUID teamId, String lang) {
    Payment payment = paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, teamId);
    Contract contract = contractRepository.getByIdAndTeamId(payment.getContractId(), teamId);
    Contact contact =
        payment
            .getContactId()
            .flatMap(cid -> contactRepository.findByIdAndTeamId(cid, teamId))
            .or(() -> contractPartyService.findPrimaryContactForContract(contract.getId(), teamId))
            .orElseThrow(
                () -> new BusinessRuleException("No tenant contact is linked to this payment"));
    String currency = payment.getAmount().currency();
    BigDecimal outstanding =
        payment
            .getAmount()
            .value()
            .subtract(
                receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId, currency));
    LocalDate today = LocalDate.now(clock);
    int daysOverdue =
        (int) Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(payment.getDueDate(), today));
    return renderFormalNotice(
        new FormalNoticeData(payment, contract, contact, outstanding, daysOverdue, lang));
  }

  @Override
  public byte[] renderFormalNotice(FormalNoticeData data) {
    Locale locale = LetterTemplateService.resolveLocale(data.languageTag());
    UUID teamId = data.contract().getTeamId();
    Property property =
        propertyRepository.getByIdAndTeamId(data.contract().getPropertyId(), teamId);
    Optional<ContactAddress> address = helper.findMailingAddress(data.contact().getId(), teamId);
    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", locale);
    LocalDate today = LocalDate.now(clock);
    String currency = data.payment().getAmount().currency();
    BigDecimal received = data.payment().getAmount().value().subtract(data.outstanding());

    Map<String, Object> vars = new HashMap<>();
    vars.put("generatedDate", today.format(dateFmt));
    vars.put("paymentIdentifier", data.payment().getIdentifier().map(Object::toString).orElse(""));
    vars.put(
        "contractIdentifier", data.contract().getIdentifier().map(Object::toString).orElse(""));
    vars.put("primaryContactName", data.contact().getDisplayName());
    vars.put("contactAddress", helper.buildAddressMap(address).orElse(null));
    vars.put(
        "propertyAddress",
        property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity());
    vars.put(
        "amount",
        CurrencyUtils.formatCurrency(data.payment().getAmount().value(), currency, locale));
    vars.put("received", CurrencyUtils.formatCurrency(received, currency, locale));
    vars.put("hasPartialPayment", received.signum() > 0);
    vars.put("outstanding", CurrencyUtils.formatCurrency(data.outstanding(), currency, locale));
    vars.put("dueDate", data.payment().getDueDate().format(dateFmt));
    vars.put("daysOverdue", data.daysOverdue());
    int deadlineDays =
        resolveDeadlineDays(
            data.contract(),
            data.contract().getCountryCode().flatMap(rentRegulationRepository::findCountryByCode));
    vars.put("deadline", today.plusDays(deadlineDays).format(dateFmt));
    vars.put("deadlineDays", deadlineDays);
    vars.putAll(paymentInstructionVariables(data.contract().getId(), teamId));
    Optional<String> countryCode = data.contract().getCountryCode();
    vars.put("countryCode", countryCode.orElse(null));
    vars.put(
        "legalClause",
        helper
            .resolveLegalClause(messageSource, "notice.legal.", countryCode, locale)
            .orElse(null));
    return documentTemplateService.renderToPdf(DOCUMENT_TYPE, locale, vars);
  }

  private Map<String, Object> paymentInstructionVariables(UUID contractId, UUID teamId) {
    Map<String, Object> vars = new HashMap<>();
    Optional<ContractPaymentInstruction> current =
        cpiRepository.findCurrentByContractIdAndTeamId(contractId, teamId);
    Optional<PaymentInstruction> template =
        current
            .filter(cpi -> !cpi.getIsCustom())
            .flatMap(ContractPaymentInstruction::getPaymentInstructionId)
            .flatMap(id -> paymentInstructionRepository.findByIdAndTeamId(id, teamId));
    String iban =
        current
            .flatMap(ContractPaymentInstruction::getCustomIban)
            .or(() -> template.flatMap(PaymentInstruction::getIban))
            .orElse("");
    String holder =
        current
            .flatMap(ContractPaymentInstruction::getCustomAccountHolderName)
            .or(() -> template.flatMap(PaymentInstruction::getAccountHolderName))
            .orElse("");
    String reference =
        current
            .flatMap(ContractPaymentInstruction::getCustomPaymentReference)
            .or(() -> template.flatMap(PaymentInstruction::getPaymentReference))
            .orElse("");
    vars.put("hasInstructions", !(iban.isBlank() && holder.isBlank() && reference.isBlank()));
    vars.put("iban", iban);
    vars.put("accountHolderName", holder);
    vars.put("paymentReference", reference);
    return vars;
  }

  /**
   * Notice period precedence: the contract's own setting, then the country regulation's default,
   * then {@link #DEFAULT_DEADLINE_DAYS}.
   */
  static int resolveDeadlineDays(Contract contract, Optional<RentRegulationCountry> regulation) {
    return contract
        .getFormalNoticeDays()
        .or(() -> regulation.flatMap(RentRegulationCountry::getFormalNoticeDays))
        .orElse(DEFAULT_DEADLINE_DAYS);
  }
}
