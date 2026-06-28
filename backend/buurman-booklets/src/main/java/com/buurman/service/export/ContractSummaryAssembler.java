package com.buurman.service.export;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.Team;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.EffectiveEndDateHelper;

/**
 * Loads a contract + its property, parties, payments and term, and projects them into the variables
 * consumed by {@code contract-summary/generic.html}. The only layer that touches repositories;
 * everything it returns is display-ready (localized money/dates/enum labels).
 */
@Component
public class ContractSummaryAssembler {

  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final ContactRepository contactRepository;
  private final PaymentRepository paymentRepository;
  private final ContractExtensionRepository contractExtensionRepository;
  private final ContractPartyService contractPartyService;
  private final TeamRepository teamRepository;
  private final BookletFormatter formatter;
  private final EnumLabelResolver enumLabels;
  private final QrCodeGenerator qrCodeGenerator;
  private final Clock clock;
  private final String appBaseUrl;

  public ContractSummaryAssembler(
      ContractRepository contractRepository,
      PropertyRepository propertyRepository,
      ContactRepository contactRepository,
      PaymentRepository paymentRepository,
      ContractExtensionRepository contractExtensionRepository,
      ContractPartyService contractPartyService,
      TeamRepository teamRepository,
      BookletFormatter formatter,
      EnumLabelResolver enumLabels,
      QrCodeGenerator qrCodeGenerator,
      Clock clock,
      @Value("${booklet.app-base-url:https://app.buurman.io}") String appBaseUrl) {
    this.contractRepository = contractRepository;
    this.propertyRepository = propertyRepository;
    this.contactRepository = contactRepository;
    this.paymentRepository = paymentRepository;
    this.contractExtensionRepository = contractExtensionRepository;
    this.contractPartyService = contractPartyService;
    this.teamRepository = teamRepository;
    this.formatter = formatter;
    this.enumLabels = enumLabels;
    this.qrCodeGenerator = qrCodeGenerator;
    this.clock = clock;
    this.appBaseUrl = appBaseUrl;
  }

  public Map<String, Object> assemble(ContractIdentifier identifier, UUID teamId, Locale locale) {
    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);
    Property property = propertyRepository.getByIdAndTeamId(contract.getPropertyId(), teamId);

    List<ContractParty> parties =
        contractPartyService.getPartiesForContract(contract.getId(), teamId);
    Set<UUID> contactIds =
        parties.stream()
            .map(ContractParty::getContactId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    Map<UUID, Contact> contactMap =
        contactRepository.findByIdsAndTeamId(contactIds, teamId).stream()
            .collect(Collectors.toMap(Contact::getId, c -> c));

    String tenantName =
        parties.stream()
            .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
            .findFirst()
            .flatMap(ContractParty::getContactId)
            .map(contactMap::get)
            .map(Contact::getDisplayName)
            .orElse("—");
    String landlordName = teamRepository.findById(teamId).map(Team::getName).orElse("—");

    List<Payment> payments = paymentRepository.findByContractId(contract.getId(), teamId);
    int paid = 0;
    int pending = 0;
    int overdue = 0;
    for (Payment p : payments) {
      switch (p.getStatus()) {
        case PAID -> paid++;
        case PENDING, PARTIALLY_PAID -> pending++;
        case OVERDUE -> overdue++;
        case CANCELLED -> {
          // excluded from health bar
        }
      }
    }
    int total = paid + pending + overdue;
    Optional<Payment> nextDue =
        payments.stream()
            .filter(
                p ->
                    p.getStatus() == Payment.PaymentStatus.PENDING
                        || p.getStatus() == Payment.PaymentStatus.OVERDUE)
            .min(Comparator.comparing(Payment::getDueDate));

    List<ContractExtension> extensions =
        contractExtensionRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    Optional<LocalDate> effectiveEnd =
        EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);

    Map<String, Object> v = new HashMap<>();
    v.put("lang", locale.getLanguage());
    v.put("dir", "ltr");
    v.put("propertyAddress", address(property));
    v.put("contractIdentifier", identifier.value());
    v.put("statusCode", contract.getStatus().name());
    v.put("statusLabel", enumLabels.label(contract.getStatus(), locale));
    v.put("landlordName", landlordName);
    v.put("tenantName", tenantName);
    v.put("heroSizeClass", heroSize(Math.max(landlordName.length(), tenantName.length())));
    v.put("rent", formatter.money(contract.getRentAmount(), locale));
    v.put("rentPeriodUnit", null);
    v.put(
        "deposit",
        formatter.money(
            contract.getDepositAmount().or(contract::getSecurityDeposit).orElse(null), locale));
    v.put("frequencyLabel", enumLabels.label(contract.getPaymentFrequency(), locale));
    v.put("termLabel", enumLabels.label(contract.getContractType(), locale));
    v.put(
        "nextPaymentAmount", nextDue.map(p -> formatter.money(p.getAmount(), locale)).orElse("—"));
    v.put(
        "nextPaymentWhen",
        nextDue.map(p -> formatter.dateShort(p.getDueDate(), locale)).orElse(""));
    v.put(
        "nextPaymentDanger",
        nextDue.map(p -> p.getStatus() == Payment.PaymentStatus.OVERDUE).orElse(false));
    v.put("paidCount", paid);
    v.put("pendingCount", pending);
    v.put("overdueCount", overdue);
    v.put("paidPct", formatter.pct(paid, total));
    v.put("pendingPct", formatter.pct(pending, total));
    v.put("overduePct", formatter.pct(overdue, total));
    v.put("paymentInstruction", null);

    LocalDate today = LocalDate.now(clock);
    effectiveEnd.ifPresent(
        end -> {
          LocalDate start = contract.getStartDate();
          v.put("leaseStart", formatter.date(start, locale));
          v.put("leaseEnd", formatter.date(end, locale));
          long span = ChronoUnit.DAYS.between(start, end);
          long elapsed = ChronoUnit.DAYS.between(start, today);
          int pct = span <= 0 ? 0 : (int) Math.max(0, Math.min(100, elapsed * 100 / span));
          v.put("termElapsedPct", pct);
          v.put("termElapsedLabel", formatter.percentWhole(pct));
        });

    v.put(
        "qrDataUri", qrCodeGenerator.toSvgDataUri(appBaseUrl + "/contracts/" + identifier.value()));
    v.put("generatedMeta", formatter.date(today, locale));
    return v;
  }

  private static String address(Property property) {
    return property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity();
  }

  private static String heroSize(int longestName) {
    if (longestName <= 12) {
      return "hero-l";
    }
    if (longestName <= 20) {
      return "hero-m";
    }
    return "hero-s";
  }
}
