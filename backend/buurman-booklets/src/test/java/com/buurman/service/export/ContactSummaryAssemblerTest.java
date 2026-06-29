package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactAddress;
import com.buurman.domain.ContactType;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Payment;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.repository.ContactAddressRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.util.MoneyAmount;

@DisplayName("ContactSummaryAssembler")
@ExtendWith(MockitoExtension.class)
class ContactSummaryAssemblerTest {

  private static final UUID TEAM = UUID.randomUUID();
  private static final ContactIdentifier ID = ContactIdentifier.of("CT-5001");

  @Mock private ContactRepository contactRepository;
  @Mock private ContactAddressRepository contactAddressRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private ContractPartyService contractPartyService;

  private ContactSummaryAssembler assembler;

  @BeforeEach
  void setUp() {
    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames("classpath:messages/test-enum-labels");
    ms.setDefaultEncoding("UTF-8");
    ms.setUseCodeAsDefaultMessage(true);
    assembler =
        new ContactSummaryAssembler(
            contactRepository,
            contactAddressRepository,
            contractRepository,
            paymentRepository,
            contractPartyService,
            new BookletFormatter(),
            new EnumLabelResolver(ms),
            new QrCodeGenerator(),
            Clock.fixed(Instant.parse("2026-06-28T00:00:00Z"), ZoneOffset.UTC),
            "https://app.buurman.io");
  }

  @Test
  @DisplayName("aggregates contracts + payments and picks the primary contact channel")
  void assembles() {
    UUID contactId = UUID.randomUUID();
    Contact contact =
        Contact.builder()
            .id(contactId)
            .contactType(ContactType.INDIVIDUAL)
            .displayName("Luís Santos")
            .email(Optional.of("luis@example.com"))
            .phone(Optional.of("+31 6 12 34 56 78"))
            .build();

    Contract active =
        Contract.builder()
            .id(UUID.randomUUID())
            .propertyId(UUID.randomUUID())
            .status(Contract.ContractStatus.ACTIVE)
            .build();
    Contract expired =
        Contract.builder()
            .id(UUID.randomUUID())
            .propertyId(UUID.randomUUID())
            .status(Contract.ContractStatus.EXPIRED)
            .build();

    when(contactRepository.getByIdentifierAndTeamId(ID, TEAM)).thenReturn(contact);
    when(contractRepository.findByContactIdViaParties(contactId, TEAM))
        .thenReturn(List.of(active, expired));
    when(paymentRepository.findByContactIdAndTeamId(contactId, TEAM))
        .thenReturn(
            List.of(
                payment(Payment.PaymentStatus.PAID),
                payment(Payment.PaymentStatus.OVERDUE),
                payment(Payment.PaymentStatus.PAID)));
    when(contractPartyService.getPartiesForContract(active.getId(), TEAM))
        .thenReturn(
            List.of(
                ContractParty.builder()
                    .contactId(Optional.of(contactId))
                    .role(ContractPartyRole.PRIMARY_TENANT)
                    .build()));
    when(contactAddressRepository.findByContactId(contactId, TEAM))
        .thenReturn(
            List.of(
                ContactAddress.builder()
                    .street("Kerkstraat 14")
                    .postalCode("1017 GC")
                    .city("Amsterdam")
                    .build()));

    Map<String, Object> v = assembler.assemble(ID, TEAM, Locale.ENGLISH);

    assertThat(v.get("contactIdentifier")).isEqualTo("CT-5001");
    assertThat(v.get("contactName")).isEqualTo("Luís Santos");
    assertThat(v.get("contactTypeLabel")).isEqualTo("Individual");
    assertThat(v.get("activeContracts")).isEqualTo("1");
    assertThat(v.get("contractsCount")).isEqualTo("2");
    assertThat(v.get("propertiesCount")).isEqualTo("2");
    assertThat((String) v.get("lifetimePaid")).contains("2,000");
    assertThat(v.get("onTimeRate")).isEqualTo("67%"); // 2 paid / (2 paid + 1 overdue)
    assertThat(v.get("roleLabel")).isEqualTo("Primary Tenant");
    assertThat(v.get("primaryChannelValue")).isEqualTo("+31 6 12 34 56 78");
    assertThat((String) v.get("mailingAddress")).contains("Kerkstraat 14");
    assertThat((String) v.get("qrDataUri")).startsWith("data:image/svg+xml;base64,");
  }

  private static Payment payment(Payment.PaymentStatus status) {
    return Payment.builder()
        .id(UUID.randomUUID())
        .amount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
        .dueDate(LocalDate.parse("2026-01-01"))
        .status(status)
        .build();
  }
}
