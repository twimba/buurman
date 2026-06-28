package com.buurman.service.export;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.util.MoneyAmount;

/**
 * Loads a contact + the contracts they are party to, aggregates their payments, and projects them
 * into the variables for {@code contact-summary/generic.html}. The amber highlight is the primary
 * contact channel — the phone/email a landlord will actually use.
 */
@Component
public class ContactSummaryAssembler {

  private final ContactRepository contactRepository;
  private final ContactAddressRepository contactAddressRepository;
  private final ContractRepository contractRepository;
  private final PaymentRepository paymentRepository;
  private final ContractPartyService contractPartyService;
  private final BookletFormatter formatter;
  private final EnumLabelResolver enumLabels;
  private final QrCodeGenerator qrCodeGenerator;
  private final Clock clock;
  private final String appBaseUrl;

  public ContactSummaryAssembler(
      ContactRepository contactRepository,
      ContactAddressRepository contactAddressRepository,
      ContractRepository contractRepository,
      PaymentRepository paymentRepository,
      ContractPartyService contractPartyService,
      BookletFormatter formatter,
      EnumLabelResolver enumLabels,
      QrCodeGenerator qrCodeGenerator,
      Clock clock,
      @Value("${booklet.app-base-url:https://app.buurman.io}") String appBaseUrl) {
    this.contactRepository = contactRepository;
    this.contactAddressRepository = contactAddressRepository;
    this.contractRepository = contractRepository;
    this.paymentRepository = paymentRepository;
    this.contractPartyService = contractPartyService;
    this.formatter = formatter;
    this.enumLabels = enumLabels;
    this.qrCodeGenerator = qrCodeGenerator;
    this.clock = clock;
    this.appBaseUrl = appBaseUrl;
  }

  public Map<String, Object> assemble(ContactIdentifier identifier, UUID teamId, Locale locale) {
    Contact contact = contactRepository.getByIdentifierAndTeamId(identifier, teamId);
    List<Contract> contracts =
        contractRepository.findByContactIdViaParties(contact.getId(), teamId);

    long activeCount =
        contracts.stream().filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE).count();
    long propertiesCount = contracts.stream().map(Contract::getPropertyId).distinct().count();

    BigDecimal paidSum = BigDecimal.ZERO;
    String currency = "EUR";
    int paidCount = 0;
    int overdueCount = 0;
    for (Contract c : contracts) {
      for (Payment p : paymentRepository.findByContractId(c.getId(), teamId)) {
        if (p.getStatus() == Payment.PaymentStatus.PAID) {
          paidSum = paidSum.add(p.getAmount().value());
          currency = p.getAmount().currency();
          paidCount++;
        } else if (p.getStatus() == Payment.PaymentStatus.OVERDUE) {
          overdueCount++;
        }
      }
    }

    Map<String, Object> v = new HashMap<>();
    v.put("lang", locale.getLanguage());
    v.put("dir", "ltr");
    v.put("contactName", contact.getDisplayName());
    v.put("contactTypeLabel", enumLabels.label(contact.getContactType(), locale));
    v.put("contactIdentifier", identifier.value());
    v.put("tags", contact.getTags().stream().map(t -> enumLabels.label(t, locale)).toList());
    v.put("activeContracts", String.valueOf(activeCount));
    v.put("heroSizeClass", "hero-l");
    v.put("roleLabel", role(contact, contracts, teamId, locale));
    v.put("contractsCount", String.valueOf(contracts.size()));
    v.put("propertiesCount", String.valueOf(propertiesCount));
    v.put("lifetimePaid", formatter.money(MoneyAmount.of(paidSum, currency), locale));
    int onTimeBase = paidCount + overdueCount;
    v.put(
        "onTimeRate",
        onTimeBase == 0 ? "—" : formatter.percentWhole(formatter.pct(paidCount, onTimeBase)));

    String email = contact.getEmail().orElse(null);
    String phone = contact.getPhone().orElse(null);
    v.put("email", email);
    v.put("phone", phone);
    v.put("website", contact.getWebsite().orElse(null));
    v.put("primaryChannelValue", phone != null ? phone : (email != null ? email : "—"));
    v.put("mailingAddress", mailingAddress(contact.getId(), teamId));
    v.put("taxId", contact.getTaxNumber().orElse(null));

    v.put(
        "qrDataUri", qrCodeGenerator.toSvgDataUri(appBaseUrl + "/contacts/" + identifier.value()));
    v.put("generatedDate", formatter.date(LocalDate.now(clock), locale));
    return v;
  }

  private String role(Contact contact, List<Contract> contracts, UUID teamId, Locale locale) {
    Optional<Contract> primary =
        contracts.stream()
            .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
            .findFirst()
            .or(() -> contracts.stream().findFirst());
    return primary
        .flatMap(
            c ->
                contractPartyService.getPartiesForContract(c.getId(), teamId).stream()
                    .filter(p -> p.getContactId().map(contact.getId()::equals).orElse(false))
                    .findFirst()
                    .map(p -> enumLabels.label(p.getRole(), locale)))
        .orElse("—");
  }

  private @Nullable String mailingAddress(UUID contactId, UUID teamId) {
    return contactAddressRepository.findByContactId(contactId, teamId).stream()
        .findFirst()
        .map(a -> a.getStreet() + ", " + a.getPostalCode() + " " + a.getCity())
        .orElse(null);
  }
}
