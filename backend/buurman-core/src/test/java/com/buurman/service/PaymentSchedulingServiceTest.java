package com.buurman.service;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Contract.ContractStatus.DRAFT;
import static com.buurman.domain.Contract.ContractStatus.EXPIRED;
import static com.buurman.domain.Contract.ContractStatus.NOTICE_GIVEN;
import static com.buurman.domain.Contract.PaymentFrequency.ANNUALLY;
import static com.buurman.domain.Contract.PaymentFrequency.MONTHLY;
import static com.buurman.domain.Contract.PaymentFrequency.QUARTERLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.buurman.domain.Sid;
import com.buurman.domain.TeamPreferences;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRentPeriodRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentSchedulingService")
class PaymentSchedulingServiceTest {

  @Mock private ContractRepository contractRepository;
  @Mock private ContractExtensionRepository contractExtensionRepository;
  @Mock private ContractRentPeriodRepository rentPeriodRepository;
  @Mock private ContractPartyRepository contractPartyRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentReceivalRepository paymentReceivalRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private TeamPreferencesRepository teamPreferencesRepository;
  @Mock private AuditService auditService;

  private PaymentSchedulingService service;

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneId.of("UTC"));
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new PaymentSchedulingService(
            contractRepository,
            contractExtensionRepository,
            contractPartyRepository,
            rentPeriodRepository,
            paymentRepository,
            paymentReceivalRepository,
            teamRepository,
            teamPreferencesRepository,
            auditService,
            FIXED_CLOCK);
  }

  private Contract activeContract() {
    return Contract.builder()
        .id(CONTRACT_ID)
        .identifier(Optional.of(Sid.of("con_test12345678901234567")))
        .teamId(TEAM_ID)
        .propertyId(UUID.randomUUID())
        .contractType(Contract.ContractType.FIXED_TERM)
        .startDate(LocalDate.of(2026, 1, 1))
        .endDate(Optional.of(LocalDate.of(2027, 1, 1)))
        .rentAmount(MoneyAmount.of(new BigDecimal("1200.00"), "EUR"))
        .paymentFrequency(MONTHLY)
        .paymentDueDay(Optional.of(1))
        .status(ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  private TeamPreferences defaultPrefs() {
    return TeamPreferences.builder()
        .teamId(TEAM_ID)
        .autoGenerationEnabled(true)
        .paymentsAheadCount(3)
        .build();
  }

  @Nested
  @DisplayName("generateFuturePaymentsForContract")
  class GenerateFuturePayments {

    @Test
    @DisplayName("skips non-ACTIVE contracts")
    void skipsNonActiveContracts() {
      Contract draft = activeContract();
      draft.setStatus(DRAFT);
      when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(draft);

      int count = service.generateFuturePaymentsForContract(CONTRACT_ID, TEAM_ID, USER_ID);

      assertThat(count).isZero();
      verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("skips when auto-generation is disabled")
    void skipsWhenAutoGenerationDisabled() {
      when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(activeContract());
      TeamPreferences prefs = defaultPrefs();
      prefs.setAutoGenerationEnabled(false);
      when(teamPreferencesRepository.getByTeamId(TEAM_ID)).thenReturn(prefs);

      int count = service.generateFuturePaymentsForContract(CONTRACT_ID, TEAM_ID, USER_ID);

      assertThat(count).isZero();
    }

    @Test
    @DisplayName("generates payments respecting paymentsAheadCount with correct fields")
    void generatesPaymentsRespectingAheadCount() {
      when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(activeContract());
      when(teamPreferencesRepository.getByTeamId(TEAM_ID)).thenReturn(defaultPrefs());
      when(contractExtensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(paymentRepository.existsByContractIdAndDueDate(eq(CONTRACT_ID), any(LocalDate.class)))
          .thenReturn(false);
      when(paymentRepository.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
              });

      int count = service.generateFuturePaymentsForContract(CONTRACT_ID, TEAM_ID, USER_ID);

      assertThat(count).isEqualTo(3);

      ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
      verify(paymentRepository, atLeastOnce()).save(captor.capture());
      Payment first = captor.getAllValues().getFirst();
      assertThat(first.getAmount().value()).isEqualByComparingTo("1200.00");
      assertThat(first.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
      assertThat(first.getContractId()).isEqualTo(CONTRACT_ID);
      assertThat(first.getAutoGenerated()).isTrue();
      // MONTHLY, clock is 2026-03-15, due day 1 -> first due date = 2026-04-01
      assertThat(first.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    @DisplayName("QUARTERLY frequency generates payments 3 months apart")
    void quarterlyFrequencyThreeMonthsApart() {
      Contract contract = activeContract();
      contract.setPaymentFrequency(QUARTERLY);
      when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(contract);
      when(teamPreferencesRepository.getByTeamId(TEAM_ID)).thenReturn(defaultPrefs());
      when(contractExtensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(paymentRepository.existsByContractIdAndDueDate(eq(CONTRACT_ID), any(LocalDate.class)))
          .thenReturn(false);
      when(paymentRepository.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
              });

      int count = service.generateFuturePaymentsForContract(CONTRACT_ID, TEAM_ID, USER_ID);

      assertThat(count).isEqualTo(3);

      ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
      verify(paymentRepository, atLeastOnce()).save(captor.capture());
      List<LocalDate> dueDates = captor.getAllValues().stream().map(Payment::getDueDate).toList();
      // Clock: 2026-03-15, quarterly, due day 1 -> 2026-06-01, 2026-09-01, 2026-12-01
      assertThat(dueDates)
          .containsExactly(
              LocalDate.of(2026, 6, 1), LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("ANNUALLY frequency generates payments 12 months apart")
    void annuallyFrequencyTwelveMonthsApart() {
      Contract contract = activeContract();
      contract.setPaymentFrequency(ANNUALLY);
      contract.setEndDate(Optional.of(LocalDate.of(2029, 1, 1)));
      when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(contract);
      TeamPreferences prefs = defaultPrefs();
      prefs.setPaymentsAheadCount(2);
      when(teamPreferencesRepository.getByTeamId(TEAM_ID)).thenReturn(prefs);
      when(contractExtensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(paymentRepository.existsByContractIdAndDueDate(eq(CONTRACT_ID), any(LocalDate.class)))
          .thenReturn(false);
      when(paymentRepository.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
              });

      int count = service.generateFuturePaymentsForContract(CONTRACT_ID, TEAM_ID, USER_ID);

      assertThat(count).isEqualTo(2);

      ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
      verify(paymentRepository, atLeastOnce()).save(captor.capture());
      List<LocalDate> dueDates = captor.getAllValues().stream().map(Payment::getDueDate).toList();
      // Clock: 2026-03-15, annually, due day 1 -> 2027-03-01, 2028-03-01
      assertThat(dueDates).containsExactly(LocalDate.of(2027, 3, 1), LocalDate.of(2028, 3, 1));
    }

    @Test
    @DisplayName("contract end date stops payment generation past effectiveEndDate")
    void contractEndDateStopsGeneration() {
      // Contract ends 2026-05-01 -- with MONTHLY + 3 ahead count,
      // only first payment (2026-04-01) should fit before end date
      Contract contract = activeContract();
      contract.setEndDate(Optional.of(LocalDate.of(2026, 5, 1)));
      when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(contract);
      when(teamPreferencesRepository.getByTeamId(TEAM_ID)).thenReturn(defaultPrefs());
      when(contractExtensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(paymentRepository.existsByContractIdAndDueDate(eq(CONTRACT_ID), any(LocalDate.class)))
          .thenReturn(false);
      when(paymentRepository.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
              });

      int count = service.generateFuturePaymentsForContract(CONTRACT_ID, TEAM_ID, USER_ID);

      // Due dates: 2026-04-01, 2026-05-01, 2026-06-01
      // 2026-06-01 is after end date 2026-05-01, so it should stop
      assertThat(count).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("skips due dates with existing payments")
    void skipsExistingPayments() {
      when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(activeContract());
      when(teamPreferencesRepository.getByTeamId(TEAM_ID)).thenReturn(defaultPrefs());
      when(contractExtensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      // First due date already exists, others don't
      when(paymentRepository.existsByContractIdAndDueDate(eq(CONTRACT_ID), any(LocalDate.class)))
          .thenReturn(true, false, false);
      when(paymentRepository.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
              });

      int count = service.generateFuturePaymentsForContract(CONTRACT_ID, TEAM_ID, USER_ID);

      assertThat(count).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("handleContractStatusChange")
  class StatusChange {

    @Test
    @DisplayName("ACTIVE triggers payment generation")
    void activeTriggersGeneration() {
      when(contractRepository.getByIdAndTeamId(CONTRACT_ID, TEAM_ID)).thenReturn(activeContract());
      when(teamPreferencesRepository.getByTeamId(TEAM_ID)).thenReturn(defaultPrefs());
      when(contractExtensionRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of());
      when(paymentRepository.existsByContractIdAndDueDate(eq(CONTRACT_ID), any(LocalDate.class)))
          .thenReturn(false);
      when(paymentRepository.save(any(Payment.class)))
          .thenAnswer(
              inv -> {
                Payment p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
              });

      service.handleContractStatusChange(CONTRACT_ID, ACTIVE, TEAM_ID, USER_ID);

      verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
    }

    @Test
    @DisplayName("EXPIRED triggers cancellation of future payments")
    void expiredTriggersCancellation() {
      Payment futurePayment1 =
          Payment.builder()
              .id(UUID.randomUUID())
              .identifier(Optional.of(Sid.of("pay_test12345678901234567")))
              .teamId(TEAM_ID)
              .contractId(CONTRACT_ID)
              .amount(MoneyAmount.of(new BigDecimal("1200.00"), "EUR"))
              .dueDate(LocalDate.of(2026, 5, 1))
              .status(Payment.PaymentStatus.PENDING)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .createdBy(USER_ID)
              .updatedBy(USER_ID)
              .build();
      Payment futurePayment2 =
          Payment.builder()
              .id(UUID.randomUUID())
              .identifier(Optional.of(Sid.of("pay_test22345678901234567")))
              .teamId(TEAM_ID)
              .contractId(CONTRACT_ID)
              .amount(MoneyAmount.of(new BigDecimal("1200.00"), "EUR"))
              .dueDate(LocalDate.of(2026, 6, 1))
              .status(Payment.PaymentStatus.PENDING)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .createdBy(USER_ID)
              .updatedBy(USER_ID)
              .build();

      when(paymentRepository.findFuturePendingByContractId(CONTRACT_ID, TEAM_ID))
          .thenReturn(List.of(futurePayment1, futurePayment2));

      service.handleContractStatusChange(CONTRACT_ID, EXPIRED, TEAM_ID, USER_ID);

      verify(paymentRepository).findFuturePendingByContractId(CONTRACT_ID, TEAM_ID);
      verify(paymentRepository).softDeleteByIdAndTeamId(futurePayment1.getId(), TEAM_ID);
      verify(paymentRepository).softDeleteByIdAndTeamId(futurePayment2.getId(), TEAM_ID);
    }

    @Test
    @DisplayName("NOTICE_GIVEN does not cancel future payments")
    void noticeGivenDoesNotCancelPayments() {
      service.handleContractStatusChange(CONTRACT_ID, NOTICE_GIVEN, TEAM_ID, USER_ID);

      verify(paymentRepository, never()).findFuturePendingByContractId(any(), any());
      verify(paymentRepository, never()).softDeleteByIdAndTeamId(any(), any());
    }
  }
}
