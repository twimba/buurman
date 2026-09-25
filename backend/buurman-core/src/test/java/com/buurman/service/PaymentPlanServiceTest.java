package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.Payment.PaymentStatus;
import com.buurman.domain.Payment.PaymentType;
import com.buurman.domain.PaymentPlan;
import com.buurman.domain.PaymentPlan.Frequency;
import com.buurman.domain.PaymentReceival;
import com.buurman.domain.TeamRole;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.dto.request.CreatePaymentPlanRequest;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.PaymentMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentPlanRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
class PaymentPlanServiceTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final ContractIdentifier CONTRACT_SID =
      ContractIdentifier.of("con_01JTEST000000000000000001");
  private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

  @Mock private PaymentPlanRepository planRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentReceivalRepository receivalRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private PaymentMapper paymentMapper;
  @Mock private AuditService auditService;
  @Mock private MetricsService metricsService;

  private final Clock clock =
      Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  private PaymentPlanService service;
  private UserPrincipal principal;

  @BeforeEach
  void setUp() {
    service =
        new PaymentPlanService(
            planRepository,
            paymentRepository,
            receivalRepository,
            contractRepository,
            paymentMapper,
            auditService,
            metricsService,
            clock);
    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-id",
            "t@example.com",
            "Test",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN,
            true);
  }

  @Nested
  @DisplayName("splitAmount / dueDate")
  class Split {
    @Test
    @DisplayName("equal instalments, remainder on the last one")
    void split() {
      List<BigDecimal> parts = PaymentPlanService.splitAmount(new BigDecimal("1000.00"), 3, "EUR");
      assertThat(parts)
          .containsExactly(
              new BigDecimal("333.33"), new BigDecimal("333.33"), new BigDecimal("333.34"));
      assertThat(parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
          .isEqualByComparingTo("1000.00");
      assertThat(PaymentPlanService.splitAmount(new BigDecimal("50.00"), 1, "EUR"))
          .containsExactly(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("due dates follow the frequency from the start date")
    void dueDates() {
      LocalDate start = LocalDate.of(2026, 1, 31);
      assertThat(PaymentPlanService.dueDate(start, Frequency.MONTHLY, 1))
          .isEqualTo(LocalDate.of(2026, 2, 28));
      assertThat(PaymentPlanService.dueDate(start, Frequency.WEEKLY, 2))
          .isEqualTo(LocalDate.of(2026, 2, 14));
      assertThat(PaymentPlanService.dueDate(start, Frequency.BIWEEKLY, 1))
          .isEqualTo(LocalDate.of(2026, 2, 14));
    }
  }

  @Nested
  @DisplayName("createPlan")
  class Create {
    private Payment overdue(String amount, PaymentIdentifier id) {
      return Payment.builder()
          .id(UUID.randomUUID())
          .identifier(Optional.of(id))
          .teamId(TEAM_ID)
          .contractId(CONTRACT_ID)
          .contactId(Optional.of(UUID.randomUUID()))
          .amount(MoneyAmount.of(new BigDecimal(amount), "EUR"))
          .dueDate(TODAY.minusDays(40))
          .status(PaymentStatus.OVERDUE)
          .createdAt(Instant.EPOCH)
          .updatedAt(Instant.EPOCH)
          .build();
    }

    @Test
    @DisplayName(
        "settles covered payments with PLAN receivals, creates instalments and pauses reminders")
    void createsPlan() {
      PaymentIdentifier a = PaymentIdentifier.of("pay_01JTEST00000000000000000A");
      PaymentIdentifier b = PaymentIdentifier.of("pay_01JTEST00000000000000000B");
      Payment pa = overdue("600.00", a);
      Payment pb = overdue("500.00", b);
      Contract contract =
          Contract.builder()
              .id(CONTRACT_ID)
              .identifier(Optional.of(CONTRACT_SID))
              .teamId(TEAM_ID)
              .propertyId(UUID.randomUUID())
              .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
              .build();
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID)).thenReturn(contract);
      when(paymentRepository.findByIdentifiersAndTeamId(anyCollection(), eq(TEAM_ID)))
          .thenReturn(List.of(pa, pb));
      when(receivalRepository.sumByPaymentIdAndTeamId(pa.getId(), TEAM_ID, "EUR"))
          .thenReturn(new BigDecimal("100.00"));
      when(receivalRepository.sumByPaymentIdAndTeamId(pb.getId(), TEAM_ID, "EUR"))
          .thenReturn(BigDecimal.ZERO);
      when(planRepository.save(any(PaymentPlan.class)))
          .thenAnswer(
              inv -> {
                PaymentPlan p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
              });
      when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
      when(paymentRepository.findByPaymentPlanId(any(), eq(TEAM_ID))).thenReturn(List.of());
      when(receivalRepository.findPaymentIdsByPlanId(any(), eq(TEAM_ID))).thenReturn(List.of());
      when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

      var response =
          service.createPlan(
              CONTRACT_SID,
              new CreatePaymentPlanRequest(
                  List.of(a, b),
                  4,
                  LocalDate.of(2026, 4, 1),
                  Optional.of(Frequency.MONTHLY),
                  Optional.of("Agreed by phone"),
                  Optional.empty()),
              principal);

      assertThat(response.totalAmount()).isEqualByComparingTo("1000.00");
      ArgumentCaptor<PaymentReceival> receivals = ArgumentCaptor.forClass(PaymentReceival.class);
      verify(receivalRepository, times(2)).save(receivals.capture());
      assertThat(receivals.getAllValues())
          .allMatch(r -> r.getReceivalType() == PaymentReceival.ReceivalType.PLAN);
      assertThat(receivals.getAllValues().get(0).getAmount().value())
          .isEqualByComparingTo("500.00");
      assertThat(pa.getStatus()).isEqualTo(PaymentStatus.PAID);

      ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
      verify(paymentRepository, times(6)).save(saved.capture()); // 2 covered + 4 instalments
      List<Payment> instalments =
          saved.getAllValues().stream()
              .filter(p -> p.getPaymentType() == PaymentType.INSTALMENT)
              .toList();
      assertThat(instalments).hasSize(4);
      assertThat(
              instalments.stream()
                  .map(p -> p.getAmount().value())
                  .reduce(BigDecimal.ZERO, BigDecimal::add))
          .isEqualByComparingTo("1000.00");
      assertThat(instalments.get(3).getDueDate()).isEqualTo(LocalDate.of(2026, 7, 1));
      assertThat(contract.getRemindersPausedUntil()).contains(LocalDate.of(2026, 7, 1));
    }

    @Test
    @DisplayName("refuses payments that are not open or belong to another contract")
    void refusesInvalid() {
      PaymentIdentifier a = PaymentIdentifier.of("pay_01JTEST00000000000000000A");
      Payment paid = overdue("600.00", a);
      paid.setStatus(PaymentStatus.PAID);
      when(contractRepository.getByIdentifierAndTeamId(CONTRACT_SID, TEAM_ID))
          .thenReturn(
              Contract.builder()
                  .id(CONTRACT_ID)
                  .teamId(TEAM_ID)
                  .propertyId(UUID.randomUUID())
                  .rentAmount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
                  .build());
      when(paymentRepository.findByIdentifiersAndTeamId(anyCollection(), eq(TEAM_ID)))
          .thenReturn(List.of(paid));

      assertThatThrownBy(
              () ->
                  service.createPlan(
                      CONTRACT_SID,
                      new CreatePaymentPlanRequest(
                          List.of(a),
                          2,
                          TODAY,
                          Optional.empty(),
                          Optional.empty(),
                          Optional.empty()),
                      principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("not open");
    }
  }
}
