package com.buurman.service.export;

import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentRenderer;
import com.buurman.document.PageSpec;
import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.ContractPaymentInstruction;
import com.buurman.domain.ContractRentPeriod;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentInstruction;
import com.buurman.domain.PaymentReceival;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractPaymentInstructionRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.EffectiveEndDateHelper;
import com.buurman.util.CurrencyUtils;

/**
 * Generates the multi-page contract dossier PDF. Loads the contract + property + parties + payments
 * + instructions, projects them into a localized view-model ({@link #buildModel}) and renders the
 * {@code contract-booklet/generic} Thymeleaf template via Gotenberg/Chromium. All money/dates/enum
 * labels are pre-formatted here; the template only lays out.
 */
@Component
public class ContractBookletExporter {

  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final ContactRepository contactRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentReceivalRepository paymentReceivalRepository;
  private final ContractPaymentInstructionRepository contractPaymentInstructionRepository;
  private final PaymentInstructionRepository paymentInstructionRepository;
  private final ContractRentPeriodRepository rentPeriodRepository;
  private final ContractExtensionRepository contractExtensionRepository;
  private final ContractPartyService contractPartyService;
  private final DocumentRenderer pdfRenderer;
  private final TemplateEngine templateEngine;
  private final MessageSource messageSource;
  private final EnumLabelResolver enumLabels;
  private final BookletFormatter formatter;
  private final QrCodeGenerator qrCodeGenerator;
  private final Clock clock;
  private final String appBaseUrl;

  public ContractBookletExporter(
      ContractRepository contractRepository,
      PropertyRepository propertyRepository,
      UnitRepository unitRepository,
      ContactRepository contactRepository,
      PaymentRepository paymentRepository,
      PaymentReceivalRepository paymentReceivalRepository,
      ContractPaymentInstructionRepository contractPaymentInstructionRepository,
      PaymentInstructionRepository paymentInstructionRepository,
      ContractRentPeriodRepository rentPeriodRepository,
      ContractExtensionRepository contractExtensionRepository,
      ContractPartyService contractPartyService,
      DocumentRenderer pdfRenderer,
      @Qualifier("contractBookletTemplateEngine") TemplateEngine templateEngine,
      @Qualifier("contractBookletMessageSource") MessageSource messageSource,
      EnumLabelResolver enumLabels,
      BookletFormatter formatter,
      QrCodeGenerator qrCodeGenerator,
      Clock clock,
      @Value("${booklet.app-base-url:https://app.buurman.io}") String appBaseUrl) {
    this.contractRepository = contractRepository;
    this.propertyRepository = propertyRepository;
    this.unitRepository = unitRepository;
    this.contactRepository = contactRepository;
    this.paymentRepository = paymentRepository;
    this.paymentReceivalRepository = paymentReceivalRepository;
    this.contractPaymentInstructionRepository = contractPaymentInstructionRepository;
    this.paymentInstructionRepository = paymentInstructionRepository;
    this.rentPeriodRepository = rentPeriodRepository;
    this.contractExtensionRepository = contractExtensionRepository;
    this.contractPartyService = contractPartyService;
    this.pdfRenderer = pdfRenderer;
    this.templateEngine = templateEngine;
    this.messageSource = messageSource;
    this.enumLabels = enumLabels;
    this.formatter = formatter;
    this.qrCodeGenerator = qrCodeGenerator;
    this.clock = clock;
    this.appBaseUrl = appBaseUrl;
  }

  private String msg(String key, Locale locale) {
    return Objects.requireNonNullElse(messageSource.getMessage(key, null, key, locale), key);
  }

  public byte[] generate(ContractIdentifier contractIdentifier, UUID teamId, Locale locale) {
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);
    Unit unit = unitRepository.getByIdAndTeamId(contract.getUnitId(), teamId);

    // Load parties + contacts
    List<ContractParty> parties =
        contractPartyService.getPartiesForContract(contract.getId(), teamId);
    Set<UUID> contactIds = new HashSet<>();
    for (ContractParty party : parties) {
      party.getContactId().ifPresent(contactIds::add);
    }
    Map<UUID, Contact> contactMap =
        contactRepository.findByIdsAndTeamId(contactIds, teamId).stream()
            .collect(toMap(Contact::getId, t -> t));

    // Load payments + receivals
    List<Payment> payments = paymentRepository.findByContractId(contract.getId(), teamId);
    Set<UUID> paymentIds = new HashSet<>();
    for (Payment p : payments) {
      paymentIds.add(p.getId());
    }
    List<PaymentReceival> allReceivals =
        paymentIds.isEmpty()
            ? List.of()
            : paymentReceivalRepository.findByPaymentIdsAndTeamId(paymentIds, teamId);
    Map<UUID, BigDecimal> receivedByPayment = new HashMap<>();
    for (PaymentReceival r : allReceivals) {
      receivedByPayment.merge(r.getPaymentId(), r.getAmount().value(), BigDecimal::add);
    }

    // Load rent periods
    List<ContractRentPeriod> rentPeriods =
        rentPeriodRepository.findByContractIdAndTeamId(contract.getId(), teamId);

    // Load payment instructions
    List<ContractPaymentInstruction> allCpis =
        contractPaymentInstructionRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    Set<UUID> piIds = new HashSet<>();
    for (ContractPaymentInstruction cpi : allCpis) {
      if (!cpi.getIsCustom()) {
        cpi.getPaymentInstructionId().ifPresent(piIds::add);
      }
    }
    Map<UUID, PaymentInstruction> piMap =
        piIds.isEmpty()
            ? Map.of()
            : paymentInstructionRepository.findAllByTeamId(teamId).stream()
                .filter(pi -> piIds.contains(pi.getId()))
                .collect(toMap(PaymentInstruction::getId, pi -> pi));

    // Compute effective end date (latest active extension overrides contract end date)
    List<ContractExtension> extensions =
        contractExtensionRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    Optional<LocalDate> effectiveEndDate =
        EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);

    Map<String, Object> model =
        buildModel(
            contract,
            property,
            unit,
            parties,
            contactMap,
            payments,
            receivedByPayment,
            rentPeriods,
            allCpis,
            piMap,
            effectiveEndDate,
            locale);
    Context context = new Context(locale);
    context.setVariables(model);
    String html = templateEngine.process("contract-booklet/generic", context);
    return pdfRenderer.render(html, PageSpec.A4_PORTRAIT);
  }

  // ── View-model ──────────────────────────────────────────────────

  private Map<String, Object> buildModel(
      Contract contract,
      Property property,
      Unit unit,
      List<ContractParty> parties,
      Map<UUID, Contact> contactMap,
      List<Payment> payments,
      Map<UUID, BigDecimal> receivedByPayment,
      List<ContractRentPeriod> rentPeriods,
      List<ContractPaymentInstruction> allCpis,
      Map<UUID, PaymentInstruction> piMap,
      Optional<LocalDate> effectiveEndDate,
      Locale locale) {
    String ccy = contract.getRentAmount().currency();
    String identifier = contract.getIdentifier().map(Sid::value).orElse("—");
    String tenantName =
        findPrimaryContact(parties, contactMap).map(Contact::getDisplayName).orElse("—");
    PaymentAggregation agg = aggregatePayments(payments, receivedByPayment);

    Map<String, Object> v = new HashMap<>();
    v.put("lang", locale.getLanguage());
    v.put("dir", "ltr");
    v.put("contractIdentifier", identifier);
    v.put("statusCode", contract.getStatus() != null ? contract.getStatus().name() : "DRAFT");
    v.put(
        "statusLabel",
        contract.getStatus() != null ? enumLabels.label(contract.getStatus(), locale) : "—");
    v.put(
        "contractTypeLabel",
        contract.getContractType() != null
            ? enumLabels.label(contract.getContractType(), locale)
            : "—");
    v.put("propertyAddress", address(property));
    v.put("tenantName", tenantName);
    v.put("rent", formatter.money(contract.getRentAmount(), locale));
    v.put(
        "frequencyLabel",
        contract.getPaymentFrequency() != null
            ? enumLabels.label(contract.getPaymentFrequency(), locale)
            : "—");
    String endStr =
        effectiveEndDate
            .map(d -> formatter.date(d, locale))
            .orElse(msg("value.indefinite", locale));
    v.put("period", formatter.date(contract.getStartDate(), locale) + " — " + endStr);
    v.put("startDate", formatter.date(contract.getStartDate(), locale));
    v.put("endDate", endStr);
    v.put("signedDate", contract.getSignedDate().map(d -> formatter.date(d, locale)).orElse(null));
    v.put("deposit", contract.getDepositAmount().map(a -> formatter.money(a, locale)).orElse(null));
    v.put(
        "securityDeposit",
        contract.getSecurityDeposit().map(a -> formatter.money(a, locale)).orElse(null));
    v.put("dueDay", contract.getPaymentDueDay().map(String::valueOf).orElse(null));
    v.put("renewalMode", enumLabels.label(contract.getRenewalMode(), locale));
    v.put(
        "lateFee",
        contract
            .getLateFeePercentage()
            .map(p -> p.stripTrailingZeros().toPlainString() + "%")
            .orElse(null));

    v.put("propertyTypeLabel", enumLabels.label(property.getPropertyType(), locale));
    v.put("propertyCategoryLabel", enumLabels.label(property.getPropertyCategory(), locale));
    // Area moved from properties to units in V068. A contract always belongs to exactly one unit,
    // so this is a direct read — no aggregation needed, unlike the property-level exports.
    v.put(
        "propertyArea",
        unit.getAreaValue().map(a -> a + " " + unit.getAreaUnit().orElse("sqm")).orElse(null));

    v.put("rentPeriods", buildRentPeriods(rentPeriods, locale));
    v.put(
        "termsHtml",
        contract
            .getTermsAndConditions()
            .filter(s -> !s.isBlank())
            .map(BookletHelper::sanitizeRichText)
            .orElse(null));
    v.put(
        "notesHtml",
        contract
            .getNotes()
            .filter(s -> !s.isBlank())
            .map(BookletHelper::sanitizeRichText)
            .orElse(null));

    v.put("parties", buildParties(parties, contactMap, locale));
    v.put("instructions", buildInstructions(allCpis, piMap, locale));
    v.put("payments", buildPayments(payments, receivedByPayment, ccy, locale));
    v.put("totalPaid", CurrencyUtils.formatCurrency(agg.totalPaid, ccy, locale));
    v.put("totalPending", CurrencyUtils.formatCurrency(agg.totalPending, ccy, locale));
    v.put("totalOverdue", CurrencyUtils.formatCurrency(agg.totalOverdue, ccy, locale));
    v.put("overdueCount", (int) agg.countOverdue);

    v.put("qrDataUri", qrCodeGenerator.toSvgDataUri(appBaseUrl + "/contracts/" + identifier));
    v.put("generatedDate", formatter.date(LocalDate.now(clock), locale));
    return v;
  }

  private List<Map<String, Object>> buildRentPeriods(
      List<ContractRentPeriod> rentPeriods, Locale locale) {
    List<ContractRentPeriod> sorted = new ArrayList<>(rentPeriods);
    sorted.sort(Comparator.comparing(ContractRentPeriod::getEffectiveFrom).reversed());
    List<Map<String, Object>> out = new ArrayList<>();
    for (ContractRentPeriod rp : sorted) {
      Map<String, Object> m = new HashMap<>();
      m.put("from", formatter.date(rp.getEffectiveFrom(), locale));
      m.put("to", rp.getEffectiveTo().map(d -> formatter.date(d, locale)).orElse("—"));
      m.put("amount", formatter.money(rp.getRentAmount(), locale));
      out.add(m);
    }
    return out;
  }

  private List<Map<String, Object>> buildParties(
      List<ContractParty> parties, Map<UUID, Contact> contactMap, Locale locale) {
    List<ContractParty> sorted = new ArrayList<>(parties);
    sorted.sort(
        Comparator.comparing((ContractParty p) -> p.getRole() != ContractPartyRole.PRIMARY_TENANT)
            .thenComparing(p -> p.getRole()));
    List<Map<String, Object>> out = new ArrayList<>();
    for (ContractParty party : sorted) {
      Contact c = party.getContactId().map(contactMap::get).orElse(null);
      if (c == null) {
        continue;
      }
      List<String> contactBits = new ArrayList<>();
      c.getEmail().filter(s -> !s.isBlank()).ifPresent(contactBits::add);
      c.getPhone().filter(s -> !s.isBlank()).ifPresent(contactBits::add);
      c.getTaxNumber()
          .filter(s -> !s.isBlank())
          .ifPresent(t -> contactBits.add(msg("summary.contact.taxId", locale) + ": " + t));
      c.getIdentifier().ifPresent(id -> contactBits.add(id.value()));
      Map<String, Object> m = new HashMap<>();
      m.put("role", enumLabels.label(party.getRole(), locale));
      m.put("name", c.getDisplayName());
      m.put("contact", String.join(" · ", contactBits));
      out.add(m);
    }
    return out;
  }

  private List<Map<String, Object>> buildInstructions(
      List<ContractPaymentInstruction> allCpis,
      Map<UUID, PaymentInstruction> piMap,
      Locale locale) {
    List<ContractPaymentInstruction> sorted = new ArrayList<>(allCpis);
    // Current (no effective-to) first, then most recent.
    sorted.sort(
        Comparator.comparing((ContractPaymentInstruction c) -> c.getEffectiveTo().isPresent())
            .thenComparing(c -> c.getEffectiveFrom(), Comparator.reverseOrder()));
    List<Map<String, Object>> out = new ArrayList<>();
    for (ContractPaymentInstruction cpi : sorted) {
      boolean isCustom = cpi.getIsCustom();
      PaymentInstruction tpl =
          !isCustom ? cpi.getPaymentInstructionId().map(piMap::get).orElse(null) : null;
      String method =
          isCustom
              ? cpi.getCustomPaymentMethod().orElse(null)
              : (tpl != null && tpl.getPaymentMethod() != null
                  ? tpl.getPaymentMethod().name()
                  : null);
      Map<String, Object> m = new HashMap<>();
      m.put(
          "name",
          isCustom ? cpi.getCustomName().orElse(null) : (tpl != null ? tpl.getName() : null));
      m.put("method", formatPaymentMethod(method, locale));
      m.put("current", cpi.getEffectiveTo().isEmpty());
      m.put(
          "bankName",
          isCustom
              ? cpi.getCustomBankName().orElse(null)
              : opt(tpl, PaymentInstruction::getBankName));
      m.put(
          "accountHolder",
          isCustom
              ? cpi.getCustomAccountHolderName().orElse(null)
              : opt(tpl, PaymentInstruction::getAccountHolderName));
      m.put(
          "iban",
          isCustom ? cpi.getCustomIban().orElse(null) : opt(tpl, PaymentInstruction::getIban));
      m.put(
          "bic",
          isCustom
              ? cpi.getCustomBicSwift().orElse(null)
              : opt(tpl, PaymentInstruction::getBicSwift));
      m.put(
          "reference",
          isCustom
              ? cpi.getCustomPaymentReference().orElse(null)
              : opt(tpl, PaymentInstruction::getPaymentReference));
      out.add(m);
    }
    return out;
  }

  private List<Map<String, Object>> buildPayments(
      List<Payment> payments, Map<UUID, BigDecimal> receivedByPayment, String ccy, Locale locale) {
    List<Payment> sorted = new ArrayList<>(payments);
    sorted.sort(Comparator.comparing(Payment::getDueDate).reversed());
    List<Map<String, Object>> out = new ArrayList<>();
    for (Payment p : sorted) {
      BigDecimal received = receivedByPayment.getOrDefault(p.getId(), BigDecimal.ZERO);
      if (received.signum() == 0 && p.getStatus() == Payment.PaymentStatus.PAID) {
        received = p.getAmount().value();
      }
      Map<String, Object> m = new HashMap<>();
      m.put("due", formatter.date(p.getDueDate(), locale));
      m.put("status", enumLabels.label(p.getStatus(), locale));
      m.put("statusCode", p.getStatus().name());
      m.put("amount", formatter.money(p.getAmount(), locale));
      m.put("paid", CurrencyUtils.formatCurrency(received, ccy, locale));
      out.add(m);
    }
    return out;
  }

  // ── Helpers ─────────────────────────────────────────────────────

  private static @Nullable String opt(
      @Nullable PaymentInstruction tpl,
      java.util.function.Function<PaymentInstruction, Optional<String>> getter) {
    return tpl == null ? null : getter.apply(tpl).orElse(null);
  }

  private Optional<Contact> findPrimaryContact(
      List<ContractParty> parties, Map<UUID, Contact> contactMap) {
    return parties.stream()
        .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
        .findFirst()
        .flatMap(p -> p.getContactId().map(contactMap::get));
  }

  private PaymentAggregation aggregatePayments(
      List<Payment> payments, Map<UUID, BigDecimal> receivedByPayment) {
    PaymentAggregation agg = new PaymentAggregation();
    for (Payment p : payments) {
      BigDecimal received = receivedByPayment.getOrDefault(p.getId(), BigDecimal.ZERO);
      switch (p.getStatus()) {
        case PAID -> {
          agg.totalPaid = agg.totalPaid.add(p.getAmount().value());
          agg.countPaid++;
        }
        case PENDING -> {
          agg.totalPending = agg.totalPending.add(p.getAmount().value());
          agg.countPending++;
        }
        case OVERDUE -> {
          agg.totalOverdue = agg.totalOverdue.add(p.getAmount().value());
          agg.countOverdue++;
        }
        case CANCELLED -> agg.countCancelled++;
        case PARTIALLY_PAID -> {
          agg.totalPaid = agg.totalPaid.add(received);
          agg.totalPending = agg.totalPending.add(p.getAmount().value().subtract(received));
          agg.countPartial++;
        }
      }
    }
    return agg;
  }

  private String formatPaymentMethod(@Nullable String value, Locale locale) {
    return enumLabels.label("paymentMethod", value, locale);
  }

  private static String address(Property property) {
    return property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity();
  }

  private static final class PaymentAggregation {
    BigDecimal totalPaid = BigDecimal.ZERO;
    BigDecimal totalPending = BigDecimal.ZERO;
    BigDecimal totalOverdue = BigDecimal.ZERO;
    long countPaid;
    long countPending;
    long countOverdue;
    long countCancelled;
    long countPartial;
  }
}
