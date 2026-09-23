package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.dto.request.WaiveLateFeeRequest;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.Constants;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
class LateFeeServiceTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

  @Mock private PaymentRepository paymentRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private AuditService auditService;
  @Mock private MetricsService metricsService;

  private final Clock clock =
      Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  private LateFeeService service;
  private UserPrincipal principal;

  @BeforeEach
  void setUp() {
    service =
        new LateFeeService(
            paymentRepository, contractRepository, auditService, metricsService, clock);
    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-id",
            "t@example.com",
            "Test User",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN,
            true);
  }

  private static Payment rent(String amount, LocalDate due) {
    return Payment.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(Sid.of("pay_01JTEST000000000000000001")))
        .teamId(TEAM_ID)
        .contractId(CONTRACT_ID)
        .contactId(Optional.of(UUID.randomUUID()))
        .amount(MoneyAmount.of(new BigDecimal(amount), "EUR"))
        .dueDate(due)
        .status(PaymentStatus.OVERDUE)
        .createdAt(Instant.EPOCH)
        .updatedAt(Instant.EPOCH)
        .build();
  }

  private static Contract contract(boolean enabled, String pct) {
    return Contract.builder()
        .id(CONTRACT_ID)
        .teamId(TEAM_ID)
        .propertyId(UUID.randomUUID())
        .lateFeeEnabled(enabled)
        .lateFeePercentage(Optional.of(new BigDecimal(pct)))
        .build();
  }

  @Nested
  @DisplayName("computeFee")
  class ComputeFee {
    @Test
    @DisplayName("percentage of the face value, rounded to cents")
    void percentageOfFaceValue() {
      assertThat(LateFeeService.computeFee(rent("1234.56", TODAY), contract(true, "2.5")))
          .contains(new BigDecimal("30.86"));
    }

    @Test
    @DisplayName("nothing when disabled or zero percent")
    void nothingWhenOff() {
      assertThat(LateFeeService.computeFee(rent("1000.00", TODAY), contract(false, "2.5")))
          .isEmpty();
      assertThat(LateFeeService.computeFee(rent("1000.00", TODAY), contract(true, "0"))).isEmpty();
    }
  }

  @Nested
  @DisplayName("runDailyLateFees")
  class Run {
    @Test
    @DisplayName("charges one LATE_FEE payment per candidate and skips already-charged ones")
    void chargesOnce() {
      Payment fresh = rent("1000.00", TODAY.minusDays(10));
      Payment alreadyCharged = rent("800.00", TODAY.minusDays(20));
      when(paymentRepository.findLateFeeCandidates(TODAY))
          .thenReturn(List.of(fresh, alreadyCharged));
      when(paymentRepository.hasLateFee(fresh.getId(), TEAM_ID)).thenReturn(false);
      when(paymentRepository.hasLateFee(alreadyCharged.getId(), TEAM_ID)).thenReturn(true);
      when(contractRepository.findByIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.of(contract(true, "5")));
      when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

      int charged = service.runDailyLateFees();

      assertThat(charged).isEqualTo(1);
      ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
      verify(paymentRepository).save(captor.capture());
      Payment fee = captor.getValue();
      assertThat(fee.getPaymentType()).isEqualTo(PaymentType.LATE_FEE);
      assertThat(fee.getParentPaymentId()).contains(fresh.getId());
      assertThat(fee.getAmount().value()).isEqualByComparingTo("50.00");
      assertThat(fee.getDueDate()).isEqualTo(TODAY);
      assertThat(fee.getStatus()).isEqualTo(PaymentStatus.PENDING);
      assertThat(fee.getContactId()).isEqualTo(fresh.getContactId());
      assertThat(fee.getCreatedBy()).isEqualTo(Constants.SYSTEM_USER_ID);
      assertThat(fee.getAutoGenerated()).isTrue();
    }
  }

  @Nested
  @DisplayName("waiveLateFee")
  class Waive {
    private final PaymentIdentifier id = PaymentIdentifier.of("pay_01JTEST00000000000000FEE1");

    private Payment fee(PaymentStatus status) {
      Payment p = rent("50.00", TODAY);
      p.setIdentifier(Optional.of(id));
      p.setPaymentType(PaymentType.LATE_FEE);
      p.setStatus(status);
      return p;
    }

    @Test
    @DisplayName("cancels the fee and records who waived it and why")
    void waives() {
      when(paymentRepository.getByIdentifierAndTeamId(id, TEAM_ID))
          .thenReturn(fee(PaymentStatus.PENDING));
      when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

      Payment saved = service.waiveLateFee(id, new WaiveLateFeeRequest("Goodwill"), principal);

      assertThat(saved.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
      assertThat(saved.getWaiveReason()).contains("Goodwill");
      assertThat(saved.getWaivedBy()).contains(USER_ID);
      assertThat(saved.getWaivedAt()).contains(Instant.now(clock));
      verify(auditService)
          .logUpdate(eq(TEAM_ID), eq("PAYMENT"), any(), eq(USER_ID), any(), any(), any());
    }

    @Test
    @DisplayName("only LATE_FEE payments can be waived, and not once paid")
    void rejects() {
      Payment rentPayment = rent("1000.00", TODAY);
      rentPayment.setIdentifier(Optional.of(id));
      when(paymentRepository.getByIdentifierAndTeamId(id, TEAM_ID)).thenReturn(rentPayment);
      assertThatThrownBy(() -> service.waiveLateFee(id, new WaiveLateFeeRequest("x"), principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Only late fees");

      when(paymentRepository.getByIdentifierAndTeamId(id, TEAM_ID))
          .thenReturn(fee(PaymentStatus.PAID));
      assertThatThrownBy(() -> service.waiveLateFee(id, new WaiveLateFeeRequest("x"), principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("paid");
      verify(paymentRepository, never()).save(any());
    }
  }
}
