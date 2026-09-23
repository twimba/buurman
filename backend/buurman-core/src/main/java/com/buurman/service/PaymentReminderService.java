package com.buurman.service;

import static com.buurman.domain.NotificationType.PAYMENT_REMINDER;
import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.util.SidGenerator.newPaymentReminderId;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractPaymentInstruction;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationUrgency;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentInstruction;
import com.buurman.domain.PaymentReminder;
import com.buurman.domain.PaymentReminder.ReminderType;
import com.buurman.domain.User;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.dto.request.BulkSendPaymentRemindersRequest;
import com.buurman.dto.request.SendPaymentReminderRequest;
import com.buurman.dto.response.BulkActionResult;
import com.buurman.dto.response.PaymentReminderResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.NotFoundException;
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
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tenant-facing payment reminders. A reminder is an email to the tenant linked to a payment, with
 * the outstanding balance and the landlord's payment instructions, recorded in the payment's
 * communications timeline.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentReminderService {

  static final String TEMPLATE_NAME = "payment-reminder-tenant";

  private final PaymentRepository paymentRepository;
  private final PaymentReceivalRepository receivalRepository;
  private final PaymentReminderRepository reminderRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final ContactRepository contactRepository;
  private final ContractPartyService contractPartyService;
  private final ContractPaymentInstructionRepository cpiRepository;
  private final PaymentInstructionRepository paymentInstructionRepository;
  private final TeamRepository teamRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final AuditService auditService;
  private final MetricsService metricsService;
  private final AppProperties appProperties;
  private final Clock clock;
  private final PlatformTransactionManager transactionManager;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentReminderResponse sendReminder(
      PaymentIdentifier identifier, SendPaymentReminderRequest request, UserPrincipal principal) {
    return performSend(identifier, request, principal);
  }

  /**
   * Sends one reminder per payment in its own transaction so one failure does not block the rest.
   */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<BulkActionResult<PaymentReminderResponse>> bulkSendReminders(
      BulkSendPaymentRemindersRequest request, UserPrincipal principal) {
    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    SendPaymentReminderRequest single = new SendPaymentReminderRequest(request.notes());
    List<BulkActionResult<PaymentReminderResponse>> results = new ArrayList<>();
    for (PaymentIdentifier identifier : request.identifiers().stream().distinct().toList()) {
      try {
        PaymentReminderResponse response =
            txTemplate.execute(status -> performSend(identifier, single, principal));
        results.add(BulkActionResult.success(identifier.value(), response));
      } catch (Exception e) {
        log.warn("Bulk reminder failed for payment {}: {}", identifier, e.getMessage());
        results.add(BulkActionResult.error(identifier.value(), errorMessage(e)));
      }
    }
    return results;
  }

  @Transactional(readOnly = true)
  public List<PaymentReminderResponse> getReminders(
      PaymentIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);
    List<PaymentReminder> reminders =
        reminderRepository.findByPaymentIdAndTeamId(payment.getId(), teamId);
    Map<UUID, Optional<String>> names = new HashMap<>();
    return reminders.stream()
        .map(
            r ->
                toResponse(
                    r,
                    names.computeIfAbsent(
                        r.getCreatedBy(),
                        id -> userRepository.findById(id).map(User::getFullName))))
        .toList();
  }

  private PaymentReminderResponse performSend(
      PaymentIdentifier identifier, SendPaymentReminderRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    LocalDate today = LocalDate.now(clock);

    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);
    if (payment.getStatus() == PAID) {
      throw new BusinessRuleException("Payment is already paid");
    }
    if (payment.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Cannot send a reminder for a cancelled payment");
    }

    String currency = payment.getAmount().currency();
    BigDecimal received =
        receivalRepository.sumByPaymentIdAndTeamId(payment.getId(), teamId, currency);
    BigDecimal outstanding = payment.getAmount().value().subtract(received);
    if (outstanding.compareTo(ZERO) <= 0) {
      throw new BusinessRuleException("Payment has no outstanding balance");
    }

    Contract contract =
        contractRepository
            .findByIdAndTeamId(payment.getContractId(), teamId)
            .orElseThrow(() -> new NotFoundException("Contract not found"));

    Contact contact =
        payment
            .getContactId()
            .flatMap(cid -> contactRepository.findByIdAndTeamId(cid, teamId))
            .or(() -> contractPartyService.findPrimaryContactForContract(contract.getId(), teamId))
            .orElseThrow(
                () -> new BusinessRuleException("No tenant contact is linked to this payment"));

    String email =
        contact
            .getInvoiceEmail()
            .filter(e -> !e.isBlank())
            .or(() -> contact.getEmail().filter(e -> !e.isBlank()))
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Tenant " + contact.getDisplayName() + " has no email address"));

    int daysOverdue = (int) Math.max(0, ChronoUnit.DAYS.between(payment.getDueDate(), today));
    String languageTag = contract.getDocumentLanguages().stream().findFirst().orElse("en");
    Locale locale = Locale.forLanguageTag(languageTag);
    String propertyName =
        propertyRepository
            .findByIdAndTeamId(contract.getPropertyId(), teamId)
            .map(p -> p.getStreet() + ", " + p.getCity())
            .orElse("");
    String teamName = teamRepository.getById(teamId).getName();
    String paymentUrl =
        appProperties.email().baseUrl()
            + "/payments/"
            + payment.getIdentifier().map(Object::toString).orElse("");

    Map<String, Object> variables = new HashMap<>();
    variables.put("contactName", contact.getDisplayName());
    variables.put("propertyName", propertyName);
    variables.put("teamName", teamName);
    variables.put(
        "amount", CurrencyUtils.formatCurrency(payment.getAmount().value(), currency, locale));
    variables.put("outstanding", CurrencyUtils.formatCurrency(outstanding, currency, locale));
    variables.put("received", CurrencyUtils.formatCurrency(received, currency, locale));
    variables.put("hasPartialPayment", received.compareTo(ZERO) > 0);
    variables.put(
        "dueDate",
        payment
            .getDueDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)));
    variables.put("daysOverdue", daysOverdue);
    variables.put("isOverdue", daysOverdue > 0);
    variables.put("notes", request.notes().filter(n -> !n.isBlank()).orElse(""));
    variables.put("baseUrl", appProperties.email().baseUrl());
    variables.put("primaryUrl", paymentUrl);
    variables.putAll(paymentInstructionVariables(contract.getId(), teamId));

    notificationService.send(
        SendNotificationRequest.builder()
            .teamId(Optional.of(teamId))
            .notificationType(PAYMENT_REMINDER)
            .recipientContactId(Optional.of(contact.getId()))
            .recipientEmail(Optional.of(email))
            .templateName(TEMPLATE_NAME)
            .templateVariables(variables)
            .languageTag(Optional.of(languageTag))
            .urgency(daysOverdue > 0 ? NotificationUrgency.URGENT : NotificationUrgency.NORMAL)
            .createdBy(principal.getUserId())
            .build());

    PaymentReminder reminder =
        PaymentReminder.builder()
            .identifier(Optional.of(newPaymentReminderId()))
            .teamId(teamId)
            .paymentId(payment.getId())
            .contactId(Optional.of(contact.getId()))
            .reminderType(ReminderType.MANUAL)
            .channel(NotificationChannel.EMAIL)
            .recipientEmail(Optional.of(email))
            .daysOverdue(daysOverdue)
            .outstandingAmount(MoneyAmount.of(outstanding, currency))
            .notes(request.notes().filter(n -> !n.isBlank()))
            .sentAt(clock.instant())
            .createdBy(principal.getUserId())
            .updatedBy(principal.getUserId())
            .build();
    reminderRepository.save(reminder);

    PaymentReminderResponse response = toResponse(reminder, Optional.of(principal.getName()));
    auditService.logCreate(
        teamId, "PAYMENT_REMINDER", reminder.getId(), principal.getUserId(), response);
    metricsService.incrementCounter(
        "payment.reminder.sent.total", "type", ReminderType.MANUAL.name(), "channel", "EMAIL");
    log.info(
        "Sent payment reminder {} for payment {} to contact {} ({} days overdue) by user {}",
        reminder.getIdentifier().orElseThrow(),
        identifier,
        contact.getIdentifier().map(Object::toString).orElse("?"),
        daysOverdue,
        principal.getUserId());
    return response;
  }

  /**
   * Resolves the contract's current payment instruction (custom or template-backed) into flat
   * template variables. Missing instruction yields {@code hasInstructions=false}.
   */
  private Map<String, Object> paymentInstructionVariables(UUID contractId, UUID teamId) {
    Map<String, Object> vars = new HashMap<>();
    Optional<ContractPaymentInstruction> current =
        cpiRepository.findCurrentByContractIdAndTeamId(contractId, teamId);
    if (current.isEmpty()) {
      vars.put("hasInstructions", false);
      return vars;
    }
    ContractPaymentInstruction cpi = current.get();
    Optional<PaymentInstruction> template =
        cpi.getIsCustom()
            ? Optional.empty()
            : cpi.getPaymentInstructionId()
                .flatMap(id -> paymentInstructionRepository.findByIdAndTeamId(id, teamId));

    String method =
        cpi.getCustomPaymentMethod()
            .or(() -> template.map(t -> t.getPaymentMethod().name()))
            .orElse("");
    String iban =
        cpi.getCustomIban().or(() -> template.flatMap(PaymentInstruction::getIban)).orElse("");
    String holder =
        cpi.getCustomAccountHolderName()
            .or(() -> template.flatMap(PaymentInstruction::getAccountHolderName))
            .orElse("");
    String bank =
        cpi.getCustomBankName()
            .or(() -> template.flatMap(PaymentInstruction::getBankName))
            .orElse("");
    String bic =
        cpi.getCustomBicSwift()
            .or(() -> template.flatMap(PaymentInstruction::getBicSwift))
            .orElse("");
    String reference =
        cpi.getCustomPaymentReference()
            .or(() -> template.flatMap(PaymentInstruction::getPaymentReference))
            .orElse("");
    String details =
        cpi.getCustomAdditionalDetails()
            .or(() -> template.flatMap(PaymentInstruction::getAdditionalDetails))
            .orElse("");

    boolean hasAny =
        !(iban.isBlank() && holder.isBlank() && reference.isBlank() && details.isBlank());
    vars.put("hasInstructions", hasAny);
    vars.put("paymentMethod", method);
    vars.put("iban", iban);
    vars.put("accountHolderName", holder);
    vars.put("bankName", bank);
    vars.put("bicSwift", bic);
    vars.put("paymentReference", reference);
    vars.put("additionalDetails", details);
    return vars;
  }

  private static PaymentReminderResponse toResponse(
      PaymentReminder reminder, Optional<String> sentByName) {
    return new PaymentReminderResponse(
        reminder.getIdentifier().orElseThrow(),
        reminder.getReminderType(),
        reminder.getChannel(),
        reminder.getRecipientEmail(),
        reminder.getDaysOverdue(),
        reminder.getOutstandingAmount().value(),
        reminder.getOutstandingAmount().currency(),
        reminder.getNotes(),
        sentByName,
        reminder.getSentAt());
  }

  private static String errorMessage(Exception e) {
    String message = e.getMessage();
    return message == null || message.isBlank() ? "An unexpected error occurred" : message;
  }
}
