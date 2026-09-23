package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.transaction.PlatformTransactionManager;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractPaymentInstruction;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationType;
import com.buurman.domain.NotificationUrgency;
import com.buurman.domain.Payment;
import com.buurman.domain.Payment.PaymentStatus;
import com.buurman.domain.PaymentReminder;
import com.buurman.domain.PaymentReminder.ReminderType;
import com.buurman.domain.PaymentReminderStep;
import com.buurman.domain.ReminderTone;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.TeamRole;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.dto.request.SendPaymentReminderRequest;
import com.buurman.dto.response.PaymentReminderResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPaymentInstructionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentReminderRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.Constants;
import com.buurman.util.MoneyAmount;

@ExtendWith(MockitoExtension.class)
class PaymentReminderServiceTest {

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final PaymentIdentifier PAYMENT_SID =
      PaymentIdentifier.of("pay_01JTEST000000000000000001");
  private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentReceivalRepository receivalRepository;
  @Mock private PaymentReminderRepository reminderRepository;
  @Mock private ContractRepository contractRepository;
  @Mock private PropertyRepository propertyRepository;
  @Mock private ContactRepository contactRepository;
  @Mock private ContractPartyService contractPartyService;
  @Mock private ContractPaymentInstructionRepository cpiRepository;
  @Mock private PaymentInstructionRepository paymentInstructionRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private UserRepository userRepository;
  @Mock private NotificationService notificationService;
  @Mock private AuditService auditService;
  @Mock private MetricsService metricsService;
  @Mock private AppProperties appProperties;
  @Mock private PlatformTransactionManager transactionManager;

  private final Clock clock =
      Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  private PaymentReminderService service;
  private UserPrincipal principal;

  @BeforeEach
  void setUp() {
    service =
        new PaymentReminderService(
            paymentRepository,
            receivalRepository,
            reminderRepository,
            contractRepository,
            propertyRepository,
            contactRepository,
            contractPartyService,
            cpiRepository,
            paymentInstructionRepository,
            teamRepository,
            userRepository,
            notificationService,
            auditService,
            metricsService,
            appProperties,
            clock,
            transactionManager);
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

  private Payment payment(PaymentStatus status, LocalDate dueDate) {
    return Payment.builder()
        .id(PAYMENT_ID)
        .identifier(Optional.of(PAYMENT_SID))
        .teamId(TEAM_ID)
        .contractId(CONTRACT_ID)
        .contactId(Optional.of(CONTACT_ID))
        .amount(MoneyAmount.of(new BigDecimal("1000.00"), "EUR"))
        .dueDate(dueDate)
        .status(status)
        .createdAt(Instant.now(clock))
        .updatedAt(Instant.now(clock))
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  private Contact contact(Optional<String> email) {
    Contact c = new Contact();
    c.setId(CONTACT_ID);
    c.setIdentifier(Optional.of(Sid.of("cnt_01JTEST000000000000000001")));
    c.setDisplayName("Jan Jansen");
    c.setEmail(email);
    c.setPaymentRemindersEnabled(true);
    return c;
  }

  private void stubHappyPath(Payment payment, BigDecimal received) {
    org.mockito.Mockito.lenient()
        .when(paymentRepository.getByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
        .thenReturn(payment);
    when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
        .thenReturn(received);
    org.mockito.Mockito.lenient()
        .when(contractRepository.findByIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            Optional.of(
                Contract.builder()
                    .id(CONTRACT_ID)
                    .teamId(TEAM_ID)
                    .propertyId(UUID.randomUUID())
                    .documentLanguages(List.of("nl", "en"))
                    .tenantRemindersEnabled(true)
                    .build()));
    org.mockito.Mockito.lenient()
        .when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
        .thenReturn(Optional.of(contact(Optional.of("jan@example.com"))));
    when(propertyRepository.findByIdAndTeamId(any(), eq(TEAM_ID))).thenReturn(Optional.empty());
    when(teamRepository.getById(TEAM_ID)).thenReturn(Team.builder().name("Acme Rentals").build());
    when(appProperties.email())
        .thenReturn(new AppProperties.Email("no-reply@buurman.io", "Buurman", "https://app.test"));
    when(cpiRepository.findCurrentByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(
            Optional.of(
                ContractPaymentInstruction.builder()
                    .isCustom(true)
                    .customIban(Optional.of("NL91ABNA0417164300"))
                    .customPaymentReference(Optional.of("RENT-2026-03"))
                    .build()));
  }

  @Nested
  @DisplayName("sendReminder")
  class SendReminder {

    @Test
    @DisplayName("emails the tenant in the contract language and records the reminder")
    void sendsAndRecords() {
      stubHappyPath(payment(PaymentStatus.PENDING, TODAY.minusDays(10)), new BigDecimal("250.00"));

      PaymentReminderResponse response =
          service.sendReminder(
              PAYMENT_SID,
              new SendPaymentReminderRequest(Optional.of("Please pay soon")),
              principal);

      ArgumentCaptor<SendNotificationRequest> captor =
          ArgumentCaptor.forClass(SendNotificationRequest.class);
      verify(notificationService).send(captor.capture());
      SendNotificationRequest sent = captor.getValue();
      assertThat(sent.notificationType()).isEqualTo(NotificationType.PAYMENT_REMINDER);
      assertThat(sent.templateName()).isEqualTo(PaymentReminderService.TEMPLATE_NAME);
      assertThat(sent.recipientEmail()).contains("jan@example.com");
      assertThat(sent.recipientContactId()).contains(CONTACT_ID);
      assertThat(sent.recipientUserId()).isEmpty();
      assertThat(sent.languageTag()).contains("nl");
      assertThat(sent.urgency()).isEqualTo(NotificationUrgency.URGENT);
      assertThat(sent.templateVariables())
          .containsEntry("daysOverdue", 10)
          .containsEntry("isOverdue", true)
          .containsEntry("hasInstructions", true)
          .containsEntry("iban", "NL91ABNA0417164300")
          .containsEntry("paymentReference", "RENT-2026-03")
          .containsEntry("teamName", "Acme Rentals")
          .containsEntry("notes", "Please pay soon")
          .containsEntry("hasPartialPayment", true);

      ArgumentCaptor<PaymentReminder> reminderCaptor =
          ArgumentCaptor.forClass(PaymentReminder.class);
      verify(reminderRepository).save(reminderCaptor.capture());
      PaymentReminder saved = reminderCaptor.getValue();
      assertThat(saved.getReminderType()).isEqualTo(ReminderType.MANUAL);
      assertThat(saved.getChannel()).isEqualTo(NotificationChannel.EMAIL);
      assertThat(saved.getDaysOverdue()).isEqualTo(10);
      assertThat(saved.getOutstandingAmount().value()).isEqualByComparingTo("750.00");
      assertThat(saved.getContactId()).contains(CONTACT_ID);
      assertThat(saved.getSentAt()).isEqualTo(Instant.now(clock));

      assertThat(response.outstandingAmount()).isEqualByComparingTo("750.00");
      assertThat(response.sentByName()).contains("Test User");
      verify(auditService)
          .logCreate(eq(TEAM_ID), eq("PAYMENT_REMINDER"), any(), eq(USER_ID), any());
      verify(metricsService)
          .incrementCounter(
              eq("payment.reminder.sent.total"),
              anyString(),
              anyString(),
              anyString(),
              anyString());
    }

    @Test
    @DisplayName(
        "a payment not yet due is sent as a normal-urgency reminder with zero days overdue")
    void notYetDueIsNormalUrgency() {
      stubHappyPath(payment(PaymentStatus.PENDING, TODAY.plusDays(5)), BigDecimal.ZERO);

      service.sendReminder(PAYMENT_SID, SendPaymentReminderRequest.empty(), principal);

      ArgumentCaptor<SendNotificationRequest> captor =
          ArgumentCaptor.forClass(SendNotificationRequest.class);
      verify(notificationService).send(captor.capture());
      assertThat(captor.getValue().urgency()).isEqualTo(NotificationUrgency.NORMAL);
      assertThat(captor.getValue().templateVariables())
          .containsEntry("daysOverdue", 0)
          .containsEntry("isOverdue", false)
          .containsEntry("hasPartialPayment", false);
    }

    @Test
    @DisplayName("rejects a paid payment without sending anything")
    void rejectsPaid() {
      when(paymentRepository.getByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
          .thenReturn(payment(PaymentStatus.PAID, TODAY.minusDays(3)));

      assertThatThrownBy(
              () ->
                  service.sendReminder(PAYMENT_SID, SendPaymentReminderRequest.empty(), principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("already paid");
      verify(notificationService, never()).send(any());
      verify(reminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a cancelled payment")
    void rejectsCancelled() {
      when(paymentRepository.getByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID))
          .thenReturn(payment(PaymentStatus.CANCELLED, TODAY.minusDays(3)));

      assertThatThrownBy(
              () ->
                  service.sendReminder(PAYMENT_SID, SendPaymentReminderRequest.empty(), principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("cancelled");
      verify(notificationService, never()).send(any());
    }

    @Test
    @DisplayName("rejects when the tenant has no email address")
    void rejectsWithoutEmail() {
      Payment p = payment(PaymentStatus.OVERDUE, TODAY.minusDays(3));
      when(paymentRepository.getByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID)).thenReturn(p);
      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(BigDecimal.ZERO);
      when(contractRepository.findByIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(
              Optional.of(
                  Contract.builder()
                      .id(CONTRACT_ID)
                      .teamId(TEAM_ID)
                      .propertyId(UUID.randomUUID())
                      .tenantRemindersEnabled(true)
                      .build()));
      when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
          .thenReturn(Optional.of(contact(Optional.empty())));

      assertThatThrownBy(
              () ->
                  service.sendReminder(PAYMENT_SID, SendPaymentReminderRequest.empty(), principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("no email address");
      verify(notificationService, never()).send(any());
    }

    @Test
    @DisplayName("falls back to the contract's primary contact when the payment has none")
    void fallsBackToPrimaryContact() {
      Payment p = payment(PaymentStatus.PENDING, TODAY.minusDays(1));
      p.setContactId(Optional.empty());
      stubHappyPath(p, BigDecimal.ZERO);
      when(contractPartyService.findPrimaryContactForContract(CONTRACT_ID, TEAM_ID))
          .thenReturn(Optional.of(contact(Optional.of("primary@example.com"))));

      service.sendReminder(PAYMENT_SID, SendPaymentReminderRequest.empty(), principal);

      ArgumentCaptor<SendNotificationRequest> captor =
          ArgumentCaptor.forClass(SendNotificationRequest.class);
      verify(notificationService).send(captor.capture());
      assertThat(captor.getValue().recipientEmail()).contains("primary@example.com");
    }

    @Test
    @DisplayName("refuses to email a tenant when neither contract nor contact opted in")
    void refusesWithoutOptIn() {
      Payment p = payment(PaymentStatus.OVERDUE, TODAY.minusDays(3));
      when(paymentRepository.getByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID)).thenReturn(p);
      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(BigDecimal.ZERO);
      when(contractRepository.findByIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(
              Optional.of(
                  Contract.builder()
                      .id(CONTRACT_ID)
                      .teamId(TEAM_ID)
                      .propertyId(UUID.randomUUID())
                      .build()));
      when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
          .thenReturn(Optional.of(contact(Optional.of("jan@example.com"))));

      assertThatThrownBy(
              () ->
                  service.sendReminder(PAYMENT_SID, SendPaymentReminderRequest.empty(), principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("not enabled on this contract");
      verify(notificationService, never()).send(any());
      verify(reminderRepository, never()).save(any());
    }

    @Test
    @DisplayName("the contract flag alone is not enough: the contact must have opted in too")
    void contractFlagAloneIsNotEnough() {
      Payment p = payment(PaymentStatus.OVERDUE, TODAY.minusDays(3));
      when(paymentRepository.getByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID)).thenReturn(p);
      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(BigDecimal.ZERO);
      when(contractRepository.findByIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(
              Optional.of(
                  Contract.builder()
                      .id(CONTRACT_ID)
                      .teamId(TEAM_ID)
                      .propertyId(UUID.randomUUID())
                      .tenantRemindersEnabled(true)
                      .build()));
      Contact notOptedIn = contact(Optional.of("jan@example.com"));
      notOptedIn.setPaymentRemindersEnabled(false);
      when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
          .thenReturn(Optional.of(notOptedIn));

      assertThatThrownBy(
              () ->
                  service.sendReminder(PAYMENT_SID, SendPaymentReminderRequest.empty(), principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("has not opted in");
      verify(notificationService, never()).send(any());
    }

    @Test
    @DisplayName("contact-level opt-in alone is not enough when the contract flag is off")
    void contactOptInAloneIsNotEnough() {
      Payment p = payment(PaymentStatus.OVERDUE, TODAY.minusDays(3));
      when(paymentRepository.getByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID)).thenReturn(p);
      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(BigDecimal.ZERO);
      when(contractRepository.findByIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(
              Optional.of(
                  Contract.builder()
                      .id(CONTRACT_ID)
                      .teamId(TEAM_ID)
                      .propertyId(UUID.randomUUID())
                      .tenantRemindersEnabled(false)
                      .build()));
      when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
          .thenReturn(Optional.of(contact(Optional.of("jan@example.com"))));

      assertThatThrownBy(
              () ->
                  service.sendReminder(PAYMENT_SID, SendPaymentReminderRequest.empty(), principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("not enabled on this contract");
      verify(notificationService, never()).send(any());
    }

    @Test
    @DisplayName("a paused contract blocks reminders even when opted in")
    void pausedContractBlocks() {
      Payment p = payment(PaymentStatus.OVERDUE, TODAY.minusDays(3));
      when(paymentRepository.getByIdentifierAndTeamId(PAYMENT_SID, TEAM_ID)).thenReturn(p);
      when(receivalRepository.sumByPaymentIdAndTeamId(PAYMENT_ID, TEAM_ID, "EUR"))
          .thenReturn(BigDecimal.ZERO);
      when(contractRepository.findByIdAndTeamId(CONTRACT_ID, TEAM_ID))
          .thenReturn(
              Optional.of(
                  Contract.builder()
                      .id(CONTRACT_ID)
                      .teamId(TEAM_ID)
                      .propertyId(UUID.randomUUID())
                      .tenantRemindersEnabled(true)
                      .remindersPausedUntil(Optional.of(TODAY.plusDays(10)))
                      .build()));
      when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
          .thenReturn(Optional.of(contact(Optional.of("jan@example.com"))));

      assertThatThrownBy(
              () ->
                  service.sendReminder(PAYMENT_SID, SendPaymentReminderRequest.empty(), principal))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("paused");
      verify(notificationService, never()).send(any());
    }

    @Test
    @DisplayName("automatic sends record the ladder step and tone with no sender name")
    void automaticRecordsStep() {
      Payment p = payment(PaymentStatus.OVERDUE, TODAY.minusDays(20));
      stubHappyPath(p, BigDecimal.ZERO);

      PaymentReminderResponse response =
          service.sendAutomatic(p, new PaymentReminderStep(14, ReminderTone.FINAL, true));

      ArgumentCaptor<PaymentReminder> captor = ArgumentCaptor.forClass(PaymentReminder.class);
      verify(reminderRepository).save(captor.capture());
      assertThat(captor.getValue().getReminderType()).isEqualTo(ReminderType.AUTOMATIC);
      assertThat(captor.getValue().getStepOffsetDays()).contains(14);
      assertThat(captor.getValue().getTone()).contains(ReminderTone.FINAL);
      assertThat(captor.getValue().getCreatedBy()).isEqualTo(Constants.SYSTEM_USER_ID);
      assertThat(response.sentByName()).isEmpty();
      assertThat(response.tone()).contains(ReminderTone.FINAL);

      ArgumentCaptor<SendNotificationRequest> sent =
          ArgumentCaptor.forClass(SendNotificationRequest.class);
      verify(notificationService).send(sent.capture());
      assertThat(sent.getValue().templateVariables())
          .containsEntry("tone", "FINAL")
          .containsEntry("isFinal", true);
    }
  }
}
