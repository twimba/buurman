package com.buurman.service.export;

import static com.buurman.domain.Payment.PaymentStatus.OVERDUE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentRenderer;
import com.buurman.document.PageSpec;
import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.ContactNote;
import com.buurman.domain.ContactRelationship;
import com.buurman.domain.ContactType;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactNoteRepository;
import com.buurman.repository.ContactRelationshipRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.EffectiveEndDateHelper;
import com.buurman.util.CurrencyUtils;

/**
 * Generates the multi-page contact dossier PDF: profile, addresses, rental history, payment
 * history, notes and relationships. Loads the data, projects it into a localized view-model ({@link
 * #buildModel}) and renders {@code contact-booklet/generic} via Gotenberg/Chromium.
 */
@Component
public class ContactBookletExporter {

  private final ContactRepository contactRepository;
  private final ContactAddressRepository contactAddressRepository;
  private final ContactNoteRepository contactNoteRepository;
  private final ContactRelationshipRepository contactRelationshipRepository;
  private final UserRepository userRepository;
  private final ContractRepository contractRepository;
  private final ContractExtensionRepository contractExtensionRepository;
  private final PaymentRepository paymentRepository;
  private final PropertyRepository propertyRepository;
  private final ContractPartyService contractPartyService;
  private final DocumentRenderer pdfRenderer;
  private final TemplateEngine templateEngine;
  private final MessageSource messageSource;
  private final EnumLabelResolver enumLabels;
  private final BookletFormatter formatter;
  private final QrCodeGenerator qrCodeGenerator;
  private final Clock clock;
  private final String appBaseUrl;

  public ContactBookletExporter(
      ContactRepository contactRepository,
      ContactAddressRepository contactAddressRepository,
      ContactNoteRepository contactNoteRepository,
      ContactRelationshipRepository contactRelationshipRepository,
      UserRepository userRepository,
      ContractRepository contractRepository,
      ContractExtensionRepository contractExtensionRepository,
      PaymentRepository paymentRepository,
      PropertyRepository propertyRepository,
      ContractPartyService contractPartyService,
      DocumentRenderer pdfRenderer,
      @Qualifier("contactBookletTemplateEngine") TemplateEngine templateEngine,
      @Qualifier("contactBookletMessageSource") MessageSource messageSource,
      EnumLabelResolver enumLabels,
      BookletFormatter formatter,
      QrCodeGenerator qrCodeGenerator,
      Clock clock,
      @Value("${booklet.app-base-url:https://app.buurman.io}") String appBaseUrl) {
    this.contactRepository = contactRepository;
    this.contactAddressRepository = contactAddressRepository;
    this.contactNoteRepository = contactNoteRepository;
    this.contactRelationshipRepository = contactRelationshipRepository;
    this.userRepository = userRepository;
    this.contractRepository = contractRepository;
    this.contractExtensionRepository = contractExtensionRepository;
    this.paymentRepository = paymentRepository;
    this.propertyRepository = propertyRepository;
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

  public byte[] generate(ContactIdentifier contactIdentifier, UUID teamId, Locale locale) {
    Contact contact = contactRepository.getByIdentifierAndTeamId(contactIdentifier, teamId);

    List<ContactAddress> addresses =
        contactAddressRepository.findByContactId(contact.getId(), teamId);
    List<Contract> contracts =
        contractRepository.findByContactIdViaParties(contact.getId(), teamId);

    List<Payment> allPayments = new ArrayList<>();
    for (Contract c : contracts) {
      allPayments.addAll(paymentRepository.findByContractId(c.getId(), teamId));
    }

    Map<UUID, Property> propertyMap = new HashMap<>();
    for (Contract c : contracts) {
      if (!propertyMap.containsKey(c.getPropertyId())) {
        propertyRepository
            .findByIdAndTeamId(c.getPropertyId(), teamId)
            .ifPresent(p -> propertyMap.put(p.getId(), p));
      }
    }

    Map<UUID, ContractPartyRole> contractRoles = new HashMap<>();
    for (Contract c : contracts) {
      contractPartyService.getPartiesForContract(c.getId(), teamId).stream()
          .filter(p -> p.getContactId().map(id -> id.equals(contact.getId())).orElse(false))
          .findFirst()
          .ifPresent(p -> contractRoles.put(c.getId(), p.getRole()));
    }

    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    List<ContractExtension> allExtensions =
        contractExtensionRepository.findByContractIdsAndTeamId(contractIds, teamId);
    Map<UUID, List<ContractExtension>> extensionsByContract =
        allExtensions.stream().collect(Collectors.groupingBy(ContractExtension::getContractId));

    List<ContactNote> notes =
        contactNoteRepository.findByContactIdAndTeamId(contact.getId(), teamId).stream()
            .sorted(Comparator.comparing(ContactNote::getOccurredAt).reversed())
            .limit(20)
            .toList();

    List<ContactRelationship> relationships =
        contactRelationshipRepository.findByContactIdAndTeamId(contact.getId(), teamId);

    Map<String, Object> model =
        buildModel(
            contact,
            addresses,
            contracts,
            allPayments,
            propertyMap,
            contractRoles,
            extensionsByContract,
            notes,
            relationships,
            teamId,
            locale);
    Context context = new Context(locale);
    context.setVariables(model);
    String html = templateEngine.process("contact-booklet/generic", context);
    return pdfRenderer.render(html, PageSpec.A4_PORTRAIT);
  }

  // ── View-model ──────────────────────────────────────────────────

  private Map<String, Object> buildModel(
      Contact contact,
      List<ContactAddress> addresses,
      List<Contract> contracts,
      List<Payment> allPayments,
      Map<UUID, Property> propertyMap,
      Map<UUID, ContractPartyRole> contractRoles,
      Map<UUID, List<ContractExtension>> extensionsByContract,
      List<ContactNote> notes,
      List<ContactRelationship> relationships,
      UUID teamId,
      Locale locale) {
    String identifier = contact.getIdentifier().map(Sid::value).orElse("—");
    boolean isIndividual = contact.getContactType() == ContactType.INDIVIDUAL;
    String currency =
        allPayments.stream().findFirst().map(p -> p.getAmount().currency()).orElse("EUR");

    BigDecimal totalPaid =
        allPayments.stream()
            .filter(p -> p.getStatus() == PAID)
            .map(p -> p.getAmount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPending =
        allPayments.stream()
            .filter(p -> p.getStatus() == PENDING || p.getStatus() == OVERDUE)
            .map(p -> p.getAmount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    long activeContracts =
        contracts.stream()
            .filter(c -> Contract.ContractStatus.IN_FORCE.contains(c.getStatus()))
            .count();
    String currentProperty = resolveCurrentProperty(contracts, propertyMap);

    Map<String, Object> v = new HashMap<>();
    v.put("lang", locale.getLanguage());
    v.put("dir", "ltr");
    v.put("contactName", contact.getDisplayName());
    v.put("contactTypeLabel", enumLabels.label(contact.getContactType(), locale));
    v.put("contactIdentifier", identifier);
    v.put("isIndividual", isIndividual);
    v.put("email", contact.getEmail().filter(s -> !s.isBlank()).orElse(null));
    v.put("phone", contact.getPhone().filter(s -> !s.isBlank()).orElse(null));
    v.put("website", contact.getWebsite().filter(s -> !s.isBlank()).orElse(null));
    v.put("companyName", contact.getCompanyName().filter(s -> !s.isBlank()).orElse(null));
    v.put("tradeName", contact.getTradeName().filter(s -> !s.isBlank()).orElse(null));
    v.put("industry", contact.getIndustry().filter(s -> !s.isBlank()).orElse(null));
    v.put("taxNumber", contact.getTaxNumber().filter(s -> !s.isBlank()).orElse(null));
    v.put("idNumber", contact.getIdNumber().filter(s -> !s.isBlank()).orElse(null));
    v.put("dateOfBirth", contact.getDateOfBirth().map(d -> formatter.date(d, locale)).orElse(null));
    v.put("currentProperty", "—".equals(currentProperty) ? null : currentProperty);
    v.put("tags", contact.getTags().stream().map(t -> enumLabels.label(t, locale)).toList());
    v.put(
        "profileNotesHtml",
        contact
            .getNotes()
            .filter(s -> !s.isBlank())
            .map(BookletHelper::sanitizeRichText)
            .orElse(null));

    v.put("totalPaid", CurrencyUtils.formatCurrency(totalPaid, currency, locale));
    v.put("outstanding", CurrencyUtils.formatCurrency(totalPending, currency, locale));
    v.put("hasOutstanding", totalPending.signum() > 0);
    v.put("activeContracts", String.valueOf(activeContracts));
    v.put("totalContracts", String.valueOf(contracts.size()));

    v.put("addresses", buildAddresses(addresses, locale));
    v.put(
        "rentals",
        buildRentals(contracts, propertyMap, contractRoles, extensionsByContract, locale));
    v.put("payments", buildPayments(allPayments, locale));
    v.put("notes", buildNotes(notes, locale));
    v.put("relationships", buildRelationships(relationships, contact, teamId, locale));

    v.put("qrDataUri", qrCodeGenerator.toSvgDataUri(appBaseUrl + "/contacts/" + identifier));
    v.put("generatedDate", formatter.date(LocalDate.now(clock), locale));
    return v;
  }

  private List<Map<String, Object>> buildAddresses(List<ContactAddress> addresses, Locale locale) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (ContactAddress a : addresses) {
      Map<String, Object> m = new HashMap<>();
      m.put(
          "type",
          a.getAddressType() != null
              ? enumLabels.label(a.getAddressType(), locale)
              : msg("value.other", locale));
      m.put("active", a.getStatus() != null && a.getStatus().name().equals("ACTIVE"));
      m.put("street", a.getStreet());
      m.put(
          "city", a.getPostalCode() != null ? a.getCity() + ", " + a.getPostalCode() : a.getCity());
      m.put("country", a.getCountryCode());
      out.add(m);
    }
    return out;
  }

  private List<Map<String, Object>> buildRentals(
      List<Contract> contracts,
      Map<UUID, Property> propertyMap,
      Map<UUID, ContractPartyRole> contractRoles,
      Map<UUID, List<ContractExtension>> extensionsByContract,
      Locale locale) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Contract c : contracts) {
      Property property = propertyMap.get(c.getPropertyId());
      ContractPartyRole role = contractRoles.get(c.getId());
      Optional<LocalDate> effEnd =
          EffectiveEndDateHelper.computeEffectiveEndDate(
              c.getEndDate(), extensionsByContract.getOrDefault(c.getId(), List.of()));
      Map<String, Object> m = new HashMap<>();
      m.put("role", role != null ? enumLabels.label(role, locale) : "—");
      m.put("status", c.getStatus() != null ? enumLabels.label(c.getStatus(), locale) : "—");
      m.put("statusCode", c.getStatus() != null ? c.getStatus().name() : "DRAFT");
      m.put(
          "property",
          property != null
              ? property.getStreet() + ", " + property.getCity()
              : msg("value.unknown", locale));
      m.put("contractId", c.getIdentifier().map(Sid::value).orElse("—"));
      m.put(
          "type",
          c.getContractType() != null ? enumLabels.label(c.getContractType(), locale) : "—");
      m.put("start", formatter.date(c.getStartDate(), locale));
      m.put("end", effEnd.map(d -> formatter.date(d, locale)).orElse(msg("value.ongoing", locale)));
      m.put("rent", formatter.money(c.getRentAmount(), locale));
      m.put(
          "frequency",
          c.getPaymentFrequency() != null
              ? enumLabels.label(c.getPaymentFrequency(), locale)
              : "—");
      out.add(m);
    }
    return out;
  }

  private List<Map<String, Object>> buildPayments(List<Payment> allPayments, Locale locale) {
    List<Payment> sorted =
        allPayments.stream()
            .sorted(
                Comparator.comparing(
                        (Payment p) -> p.getDueDate() != null ? p.getDueDate() : LocalDate.MIN)
                    .reversed())
            .limit(50)
            .toList();
    List<Map<String, Object>> out = new ArrayList<>();
    for (Payment p : sorted) {
      Map<String, Object> m = new HashMap<>();
      m.put("due", p.getDueDate() != null ? formatter.date(p.getDueDate(), locale) : "—");
      m.put("paidOn", p.getPaymentDate().map(d -> formatter.date(d, locale)).orElse("—"));
      m.put("status", p.getStatus() != null ? enumLabels.label(p.getStatus(), locale) : "—");
      m.put("statusCode", p.getStatus() != null ? p.getStatus().name() : "");
      m.put("amount", formatter.money(p.getAmount(), locale));
      out.add(m);
    }
    return out;
  }

  private List<Map<String, Object>> buildNotes(List<ContactNote> notes, Locale locale) {
    Map<UUID, String> userNames = new HashMap<>();
    for (UUID userId : notes.stream().map(ContactNote::getCreatedBy).distinct().toList()) {
      userRepository.findById(userId).ifPresent(u -> userNames.put(userId, u.getFullName()));
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (ContactNote note : notes) {
      String body = BookletHelper.sanitizeRichText(note.getBody());
      if (body.length() > 500) {
        body = body.substring(0, 500) + "…";
      }
      Map<String, Object> m = new HashMap<>();
      m.put("type", note.getInteractionType().getDisplayName());
      m.put("pinned", note.isPinned());
      m.put(
          "date",
          formatter.date(
              note.getOccurredAt().atZone(ZoneId.systemDefault()).toLocalDate(), locale));
      m.put("author", userNames.getOrDefault(note.getCreatedBy(), msg("value.unknown", locale)));
      m.put("subject", note.getSubject().filter(s -> !s.isBlank()).orElse(null));
      m.put("body", body);
      out.add(m);
    }
    return out;
  }

  private List<Map<String, Object>> buildRelationships(
      List<ContactRelationship> relationships, Contact contact, UUID teamId, Locale locale) {
    Map<UUID, String> names = new HashMap<>();
    for (ContactRelationship rel : relationships) {
      UUID otherId =
          rel.getSourceContactId().equals(contact.getId())
              ? rel.getTargetContactId()
              : rel.getSourceContactId();
      names.computeIfAbsent(
          otherId,
          id ->
              contactRepository
                  .findByIdAndTeamId(id, teamId)
                  .map(Contact::getDisplayName)
                  .orElse(msg("value.unknown", locale)));
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (ContactRelationship rel : relationships) {
      boolean source = rel.getSourceContactId().equals(contact.getId());
      UUID otherId = source ? rel.getTargetContactId() : rel.getSourceContactId();
      Map<String, Object> m = new HashMap<>();
      m.put("name", names.get(otherId));
      m.put(
          "type",
          source
              ? rel.getRelationshipType().getDisplayName()
              : rel.getRelationshipType().inverseDisplayName());
      m.put("notes", rel.getNotes().filter(s -> !s.isBlank()).orElse("—"));
      out.add(m);
    }
    return out;
  }

  private static String resolveCurrentProperty(
      List<Contract> contracts, Map<UUID, Property> propertyMap) {
    return contracts.stream()
        .filter(c -> Contract.ContractStatus.IN_FORCE.contains(c.getStatus()))
        .findFirst()
        .map(c -> propertyMap.get(c.getPropertyId()))
        .map(p -> p.getStreet() + ", " + p.getCity())
        .orElse("—");
  }
}
