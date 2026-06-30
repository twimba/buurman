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
import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.Team;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.util.MoneyAmount;

@DisplayName("ContractSummaryAssembler")
@ExtendWith(MockitoExtension.class)
class ContractSummaryAssemblerTest {

  private static final UUID TEAM = UUID.randomUUID();
  private static final ContractIdentifier ID = ContractIdentifier.of("C-2024-0187");

  @Mock private ContractRepository contractRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private ContractPartyService contractPartyService;
  @Mock private TeamRepository teamRepository;

  private ContractSummaryAssembler assembler;

  @BeforeEach
  void setUp() {
    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames("classpath:messages/test-enum-labels");
    ms.setDefaultEncoding("UTF-8");
    ms.setUseCodeAsDefaultMessage(true);
    assembler =
        new ContractSummaryAssembler(
            contractRepository,
            propertyRepository,
            paymentRepository,
            contractExtensionRepository,
            contractPartyService,
            teamRepository,
            new BookletFormatter(),
            new EnumLabelResolver(ms),
            new QrCodeGenerator(),
            Clock.fixed(Instant.parse("2026-06-28T00:00:00Z"), ZoneOffset.UTC),
            "https://app.buurman.io");
  }

  @Test
  @DisplayName("projects contract + parties + payments into localized template variables")
  void assembles() {
    UUID propertyId = UUID.randomUUID();
    UUID tenantContactId = UUID.randomUUID();

    Contract contract =
        Contract.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM)
            .propertyId(propertyId)
            .contractType(Contract.ContractType.FIXED_TERM)
            .startDate(LocalDate.parse("2024-01-01"))
            .endDate(Optional.of(LocalDate.parse("2024-12-31")))
            .rentAmount(MoneyAmount.of(new BigDecimal("1450.00"), "EUR"))
            .depositAmount(Optional.of(MoneyAmount.of(new BigDecimal("2900.00"), "EUR")))
            .paymentFrequency(Contract.PaymentFrequency.MONTHLY)
            .status(Contract.ContractStatus.ACTIVE)
            .build();
    Property property =
        Property.builder()
            .id(propertyId)
            .street("Kerkstraat 14")
            .postalCode("1017 GC")
            .city("Amsterdam")
            .build();
    Contact tenantContact =
        Contact.builder().id(tenantContactId).displayName("Luís Santos").build();
    Team team = Team.builder().id(TEAM).name("Vastgoed Bakker B.V.").build();

    List<Payment> payments =
        List.of(
            payment(Payment.PaymentStatus.PAID, "2026-04-01"),
            payment(Payment.PaymentStatus.PAID, "2026-05-01"),
            payment(Payment.PaymentStatus.OVERDUE, "2026-06-01"),
            payment(Payment.PaymentStatus.PENDING, "2026-07-01"));

    when(contractRepository.getByIdentifierAndTeamId(ID, TEAM)).thenReturn(contract);
    when(propertyRepository.getByIdAndTeamId(propertyId, TEAM)).thenReturn(property);
    when(contractPartyService.findPrimaryContactForContract(contract.getId(), TEAM))
        .thenReturn(Optional.of(tenantContact));
    when(paymentRepository.findByContractId(contract.getId(), TEAM)).thenReturn(payments);
    when(contractExtensionRepository.findByContractIdAndTeamId(contract.getId(), TEAM))
        .thenReturn(List.of());
    when(teamRepository.findById(TEAM)).thenReturn(Optional.of(team));

    Map<String, Object> v = assembler.assemble(ID, TEAM, Locale.ENGLISH);

    assertThat(v.get("contractIdentifier")).isEqualTo("C-2024-0187");
    assertThat(v.get("statusCode")).isEqualTo("ACTIVE");
    assertThat(v.get("statusLabel")).isEqualTo("Active");
    assertThat(v.get("landlordName")).isEqualTo("Vastgoed Bakker B.V.");
    assertThat(v.get("tenantName")).isEqualTo("Luís Santos");
    assertThat((String) v.get("rent")).contains("1,450");
    assertThat((String) v.get("deposit")).contains("2,900");
    assertThat(v.get("frequencyLabel")).isEqualTo("Monthly");
    assertThat(v.get("termLabel")).isEqualTo("Fixed Term");
    // 2 paid, 1 pending, 1 overdue
    assertThat(v.get("paidCount")).isEqualTo(2);
    assertThat(v.get("pendingCount")).isEqualTo(1);
    assertThat(v.get("overdueCount")).isEqualTo(1);
    assertThat(v.get("paidPct")).isEqualTo(50);
    // earliest unpaid (1 Jun, OVERDUE) drives the highlight to danger
    assertThat(v.get("nextPaymentDanger")).isEqualTo(true);
    // term progress present because an end date exists
    assertThat(v).containsKey("termElapsedPct");
    assertThat((String) v.get("qrDataUri")).startsWith("data:image/svg+xml;base64,");
  }

  private static Payment payment(Payment.PaymentStatus status, String dueDate) {
    return Payment.builder()
        .id(UUID.randomUUID())
        .amount(MoneyAmount.of(new BigDecimal("1450.00"), "EUR"))
        .dueDate(LocalDate.parse(dueDate))
        .status(status)
        .build();
  }
}
