package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.transaction.PlatformTransactionManager;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Payment;
import com.buurman.domain.Payment.PaymentStatus;
import com.buurman.domain.PaymentReceival;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.mapper.ContactMapper;
import com.buurman.mapper.ContractMapper;
import com.buurman.mapper.DocumentMapper;
import com.buurman.mapper.PaymentMapper;
import com.buurman.mapper.PaymentReceivalMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.AuditLogRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.util.MoneyAmount;

import jakarta.validation.Validator;

/**
 * Tests PaymentService private status methods via reflection. These methods are private with 24
 * constructor deps, so we construct the service manually and use reflection to invoke them
 * directly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService — status state machine")
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentReceivalRepository receivalRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContactRepository contactRepository;
  @Mock private ContractPartyService contractPartyService;
  @Mock private CurrencyEnforcementService currencyEnforcement;
  @Mock private DocumentRepository documentRepository;
  @Mock private PaymentMapper paymentMapper;
  @Mock private PaymentReceivalMapper receivalMapper;
  @Mock private ContractMapper contractMapper;
  @Mock private PropertyMapper propertyMapper;
  @Mock private ContactMapper contactMapper;
  @Mock private AuditService auditService;
  @Mock private DocumentService documentService;
  @Mock private DocumentMapper documentMapper;
  @Mock private MetricsService metricsService;
  @Mock private NotificationService notificationService;
  @Mock private S3StorageService s3StorageService;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private AppProperties appProperties;
  @Mock private PlatformTransactionManager transactionManager;
  @Mock private Validator validator;

  private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);

  private PaymentService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();

  private UserPrincipal principal;

  @BeforeEach
  void setUp() {
    service =
        new PaymentService(
            paymentRepository,
            receivalRepository,
            contractRepository,
            propertyRepository,
            contactRepository,
            contractPartyService,
            currencyEnforcement,
            documentRepository,
            paymentMapper,
            receivalMapper,
            contractMapper,
            propertyMapper,
            contactMapper,
            auditService,
            documentService,
            documentMapper,
            metricsService,
            notificationService,
            s3StorageService,
            auditLogRepository,
            appProperties,
            clock,
            transactionManager,
            validator);

    principal =
        new UserPrincipal(
            USER_ID,
            "usr_test",
            "kc-id",
            "test@example.com",
            "Test User",
            TEAM_ID,
            "team_test",
            TeamRole.TEAM_ADMIN,
            true);
  }

  private Payment buildPayment(BigDecimal amount, LocalDate dueDate, PaymentStatus status) {
    return Payment.builder()
        .id(PAYMENT_ID)
        .identifier(Optional.of(Sid.of("pay_01JTEST000000000000000001")))
        .teamId(TEAM_ID)
        .contractId(UUID.randomUUID())
        .amount(MoneyAmount.of(amount, "EUR"))
        .dueDate(dueDate)
        .status(status)
        .createdAt(Instant.now(clock))
        .updatedAt(Instant.now(clock))
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  /**
   * Invokes the private {@code recalculatePaymentStatus(Payment, UserPrincipal)} via reflection.
   */
  private void invokeRecalculate(Payment payment, UserPrincipal p) throws Exception {
    java.lang.reflect.Method method =
        PaymentService.class.getDeclaredMethod(
            "recalculatePaymentStatus", Payment.class, UserPrincipal.class);
    method.setAccessible(true);
    try {
      method.invoke(service, payment, p);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof Exception ex) {
        throw ex;
      }
      throw e;
    }
  }

  /** Invokes the private {@code updatePaymentStatus(Payment, LocalDate)} via reflection. */
  private void invokeUpdateStatus(Payment payment, LocalDate today) throws Exception {
    java.lang.reflect.Method method =
        PaymentService.class.getDeclaredMethod(
            "updatePaymentStatus", Payment.class, LocalDate.class);
    method.setAccessible(true);
    try {
      method.invoke(service, payment, today);
    } catch (java.lang.reflect.InvocationTargetException e) {
      if (e.getCause() instanceof Exception ex) {
        throw ex;
      }
      throw e;
    }
  }

  @Nested
  @DisplayName("recalculatePaymentStatus")
  class RecalculatePaymentStatus {

    @Test
    @DisplayName("PENDING when no receivals and due date is in the future")
    void pendingWhenNoReceivalsAndFutureDue() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("1000.00"), LocalDate.of(2026, 4, 1), PaymentStatus.PENDING);

      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(BigDecimal.ZERO);

      invokeRecalculate(payment, principal);

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
      assertThat(payment.getPaymentDate()).isEmpty();
      verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("OVERDUE when no receivals and due date has passed")
    void overdueWhenNoReceivalsAndPastDue() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("1000.00"), LocalDate.of(2026, 2, 1), PaymentStatus.PENDING);

      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(BigDecimal.ZERO);

      invokeRecalculate(payment, principal);

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.OVERDUE);
      assertThat(payment.getPaymentDate()).isEmpty();
    }

    @Test
    @DisplayName("PARTIALLY_PAID when some received but balance remains")
    void partiallyPaidWhenSomeReceived() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("1000.00"), LocalDate.of(2026, 4, 1), PaymentStatus.PENDING);

      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(new BigDecimal("500.00"));

      invokeRecalculate(payment, principal);

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
      assertThat(payment.getPaymentDate()).isEmpty();
    }

    @Test
    @DisplayName("PAID when total received equals amount — paymentDate set to latest receival date")
    void paidWhenFullyReceived() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("1000.00"), LocalDate.of(2026, 4, 1), PaymentStatus.PENDING);

      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(new BigDecimal("1000.00"));

      PaymentReceival receival1 =
          PaymentReceival.builder()
              .id(UUID.randomUUID())
              .receivalDate(LocalDate.of(2026, 2, 15))
              .amount(MoneyAmount.of(new BigDecimal("500.00"), "EUR"))
              .build();
      PaymentReceival receival2 =
          PaymentReceival.builder()
              .id(UUID.randomUUID())
              .receivalDate(LocalDate.of(2026, 2, 28))
              .amount(MoneyAmount.of(new BigDecimal("500.00"), "EUR"))
              .build();
      when(receivalRepository.findByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID))
          .thenReturn(List.of(receival1, receival2));

      invokeRecalculate(payment, principal);

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
      assertThat(payment.getPaymentDate()).isPresent().contains(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("PAID when overpaid — paymentDate set to latest receival date")
    void paidWhenOverpaid() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("1000.00"), LocalDate.of(2026, 4, 1), PaymentStatus.PENDING);

      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(new BigDecimal("1200.00"));

      PaymentReceival receival =
          PaymentReceival.builder()
              .id(UUID.randomUUID())
              .receivalDate(LocalDate.of(2026, 3, 1))
              .amount(MoneyAmount.of(new BigDecimal("1200.00"), "EUR"))
              .build();
      when(receivalRepository.findByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID))
          .thenReturn(List.of(receival));

      invokeRecalculate(payment, principal);

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
      assertThat(payment.getPaymentDate()).isPresent().contains(LocalDate.of(2026, 3, 1));
    }

    @Test
    @DisplayName("PARTIALLY_PAID clears paymentDate")
    void partiallyPaidClearsPaymentDate() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("1000.00"), LocalDate.of(2026, 4, 1), PaymentStatus.PAID);
      payment.setPaymentDate(Optional.of(LocalDate.of(2026, 2, 15)));

      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(new BigDecimal("500.00"));

      invokeRecalculate(payment, principal);

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
      assertThat(payment.getPaymentDate()).isEmpty();
    }

    @Test
    @DisplayName("saves payment to repository with updated fields")
    void savesToRepository() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("1000.00"), LocalDate.of(2026, 4, 1), PaymentStatus.PENDING);

      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(BigDecimal.ZERO);

      invokeRecalculate(payment, principal);

      ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
      verify(paymentRepository).save(captor.capture());
      Payment saved = captor.getValue();
      assertThat(saved.getUpdatedBy()).isEqualTo(USER_ID);
      assertThat(saved.getUpdatedAt()).isEqualTo(clock.instant());
    }
  }

  @Nested
  @DisplayName("updatePaymentStatus (PENDING -> OVERDUE)")
  class UpdatePaymentStatus {

    @Test
    @DisplayName("PENDING becomes OVERDUE when due date has passed")
    void pendingBecomesOverdue() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("500.00"), LocalDate.of(2026, 2, 15), PaymentStatus.PENDING);

      invokeUpdateStatus(payment, LocalDate.of(2026, 3, 1));

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.OVERDUE);
    }

    @Test
    @DisplayName("PENDING stays PENDING when due date is today")
    void pendingStaysPendingOnDueDate() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("500.00"), LocalDate.of(2026, 3, 1), PaymentStatus.PENDING);

      invokeUpdateStatus(payment, LocalDate.of(2026, 3, 1));

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("PAID is not changed to OVERDUE even if past due")
    void paidNotChangedToOverdue() throws Exception {
      Payment payment =
          buildPayment(new BigDecimal("500.00"), LocalDate.of(2026, 1, 1), PaymentStatus.PAID);

      invokeUpdateStatus(payment, LocalDate.of(2026, 3, 1));

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("PARTIALLY_PAID is not changed to OVERDUE")
    void partiallyPaidNotChangedToOverdue() throws Exception {
      Payment payment =
          buildPayment(
              new BigDecimal("500.00"), LocalDate.of(2026, 1, 1), PaymentStatus.PARTIALLY_PAID);

      invokeUpdateStatus(payment, LocalDate.of(2026, 3, 1));

      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
    }
  }
}
