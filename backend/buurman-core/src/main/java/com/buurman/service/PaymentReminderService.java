package com.buurman.service;

import static com.buurman.domain.NotificationType.PAYMENT_REMINDER;
import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.util.SidGenerator.newPaymentReminderId;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractPaymentInstruction;
import com.buurman.domain.Document;
import com.buurman.domain.Notification;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationUrgency;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentInstruction;
import com.buurman.domain.PaymentReminder;
import com.buurman.domain.PaymentReminder.ReminderType;
import com.buurman.domain.PaymentReminderStep;
import com.buurman.domain.ReminderDeliveryStatus;
import com.buurman.domain.ReminderTone;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.TeamPreferences;
import com.buurman.domain.User;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.dto.request.BulkSendPaymentRemindersRequest;
import com.buurman.dto.request.SendPaymentReminderRequest;
import com.buurman.dto.response.BulkActionResult;
import com.buurman.dto.response.PaymentReminderResponse;
import com.buurman.dto.response.ReminderPreviewResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPaymentInstructionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentReminderRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.document.TenantNoticeDocumentService;
import com.buurman.service.notification.EmailAttachment;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.RenderedContent;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.Constants;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.DocumentLanguages;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tenant-facing payment reminders. A reminder is an email to the tenant linked to a payment, with
 * the outstanding balance and the landlord's payment instructions, recorded in the payment's
 * communications timeline.
 *
 * <p>Reminders are opt-in: nothing is ever sent unless the contract or the tenant contact has
 * explicitly enabled them, and never while the contract's reminders are paused.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentReminderService {

  static final String TEMPLATE_NAME = "payment-reminder-tenant";

  /** Worked-example values for the settings preview; never used for a real send. */
  private static final String SAMPLE_TENANT_NAME = "Sam Example";

  private static final String SAMPLE_PROPERTY = "12 Example Street, Springfield";
  private static final String SAMPLE_IBAN = "NL00BANK0123456789";
  private static final String SAMPLE_REFERENCE = "RENT-EXAMPLE";
  private static final BigDecimal SAMPLE_RENT = new BigDecimal("1200.00");
  private static final String PDF_MIME = MediaType.APPLICATION_PDF_VALUE;
  private static final String CONTRACT_ENTITY = "CONTRACT";

  /** A tenant is not chased twice for the same payment within this window by hand. */
  static final Duration MANUAL_COOLDOWN = Duration.ofHours(24);

  /** Rendering a formal notice per payment is slow; bulk FINAL sends are capped. */
  static final int MAX_BULK_FINAL = 50;

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
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final AuditService auditService;
  private final MetricsService metricsService;
  private final AppProperties appProperties;
  private final DocumentRepository documentRepository;
  private final S3StorageService s3StorageService;
  private final ObjectProvider<TenantNoticeDocumentService> noticeDocumentService;
  private final Clock clock;
  private final PlatformTransactionManager transactionManager;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentReminderResponse sendReminder(
      PaymentIdentifier identifier, SendPaymentReminderRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);
    return send(
        payment,
        teamId,
        ReminderType.MANUAL,
        Optional.empty(),
        request.tone(),
        request.notes(),
        principal.getUserId(),
        Optional.of(principal.getName()));
  }

  /**
   * Sends one reminder per payment in its own transaction so one failure does not block the rest.
   */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<BulkActionResult<PaymentReminderResponse>> bulkSendReminders(
      BulkSendPaymentRemindersRequest request, UserPrincipal principal) {
    List<PaymentIdentifier> identifiers = request.identifiers().stream().distinct().toList();
    if (request.tone().filter(ReminderTone.FINAL::equals).isPresent()
        && identifiers.size() > MAX_BULK_FINAL) {
      throw new BadRequestException(
          "A final notice can be sent to at most " + MAX_BULK_FINAL + " payments at once");
    }
    SendPaymentReminderRequest single =
        new SendPaymentReminderRequest(request.notes(), request.tone());
    List<BulkActionResult<PaymentReminderResponse>> results = new ArrayList<>();
    for (PaymentIdentifier identifier : identifiers) {
      try {
        PaymentReminderResponse response = sendReminder(identifier, single, principal);
        results.add(BulkActionResult.success(identifier.value(), response));
      } catch (Exception e) {
        log.warn("Bulk reminder failed for payment {}: {}", identifier, e.getMessage());
        results.add(BulkActionResult.error(identifier.value(), errorMessage(e)));
      }
    }
    return results;
  }

  /**
   * Sends a ladder step on behalf of the scheduler. Same eligibility rules as a manual send; the
   * step offset is recorded so the scheduler never repeats it.
   */
  public PaymentReminderResponse sendAutomatic(Payment payment, PaymentReminderStep step) {
    return send(
        payment,
        payment.getTeamId(),
        ReminderType.AUTOMATIC,
        Optional.of(step.offsetDays()),
        Optional.of(step.tone()),
        Optional.empty(),
        Constants.SYSTEM_USER_ID,
        Optional.empty());
  }

  @Transactional(readOnly = true)
  public List<PaymentReminderResponse> getReminders(
      PaymentIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Payment payment = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);
    List<PaymentReminder> reminders =
        reminderRepository.findByPaymentIdAndTeamId(payment.getId(), teamId);
    Map<UUID, Optional<String>> names = new HashMap<>();
    Map<UUID, NotificationService.DeliveryState> delivery =
        notificationService.deliveryStates(
            reminders.stream()
                .map(PaymentReminder::getNotificationId)
                .flatMap(Optional::stream)
                .toList(),
            teamId);
    return reminders.stream()
        .map(
            r ->
                toResponse(
                    r,
                    r.getReminderType() == ReminderType.AUTOMATIC
                        ? Optional.empty()
                        : names.computeIfAbsent(
                            r.getCreatedBy(),
                            id -> userRepository.findById(id).map(User::getFullName)),
                    r.getNotificationId()
                        .map(delivery::get)
                        .map(d -> ReminderDeliveryStatus.from(d.status()))
                        .orElse(ReminderDeliveryStatus.UNKNOWN),
                    r.getNotificationId().map(delivery::get).flatMap(d -> d.error())))
        .toList();
  }

  /**
   * Whether tenant reminders may be sent for this contract/contact pair today. Both the contract
   * flag and the contact's own consent are required, and a pause on the contract blocks all sends.
   */
  public static boolean remindersEnabled(
      Contract contract, Optional<Contact> contact, LocalDate today) {
    return !isPaused(contract, today) && contractEnabled(contract) && contactOptedIn(contact);
  }

  static boolean isPaused(Contract contract, LocalDate today) {
    return contract.getRemindersPausedUntil().map(until -> !until.isBefore(today)).orElse(false);
  }

  static boolean contractEnabled(Contract contract) {
    return Boolean.TRUE.equals(contract.getTenantRemindersEnabled());
  }

  static boolean contactOptedIn(Optional<Contact> contact) {
    return contact.map(c -> Boolean.TRUE.equals(c.getPaymentRemindersEnabled())).orElse(false);
  }

  private PaymentReminderResponse send(
      Payment payment,
      UUID teamId,
      ReminderType type,
      Optional<Integer> stepOffsetDays,
      Optional<ReminderTone> requestedTone,
      Optional<String> notes,
      UUID actorUserId,
      Optional<String> actorName) {
    LocalDate today = LocalDate.now(clock);

    if (payment.getStatus() == PAID) {
      throw new BusinessRuleException("Payment is already paid");
    }
    if (payment.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Cannot send a reminder for a cancelled payment");
    }
    if (type == ReminderType.MANUAL) {
      Optional.ofNullable(
              reminderRepository
                  .summarizeByPaymentIds(List.of(payment.getId()), teamId)
                  .get(payment.getId()))
          .flatMap(PaymentReminderRepository.ReminderSummary::lastSentAt)
          .filter(last -> Duration.between(last, clock.instant()).compareTo(MANUAL_COOLDOWN) < 0)
          .ifPresent(
              last -> {
                throw new BusinessRuleException(
                    "A reminder for this payment was already sent less than "
                        + MANUAL_COOLDOWN.toHours()
                        + " hours ago");
              });
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

    if (isPaused(contract, today)) {
      throw new BusinessRuleException(
          "Reminders for this contract are paused until "
              + contract.getRemindersPausedUntil().orElseThrow());
    }
    if (!contractEnabled(contract)) {
      throw new BusinessRuleException("Tenant reminders are not enabled on this contract");
    }
    if (!contactOptedIn(Optional.of(contact))) {
      throw new BusinessRuleException(
          "Tenant " + contact.getDisplayName() + " has not opted in to payment reminders");
    }

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
    ReminderTone tone =
        requestedTone.orElse(daysOverdue > 0 ? ReminderTone.FIRM : ReminderTone.FRIENDLY);
    String languageTag = DocumentLanguages.firstSupportedOrDefault(contract.getDocumentLanguages());
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

    Map<String, Object> variables =
        reminderVariables(
            contact.getDisplayName(),
            propertyName,
            teamName,
            payment.getAmount().value(),
            received,
            outstanding,
            currency,
            payment.getDueDate(),
            daysOverdue,
            tone,
            notes,
            paymentUrl,
            locale,
            paymentInstructionVariables(contract.getId(), teamId));

    // Slow external work (Gotenberg render, S3 upload) happens before the transaction opens.
    Optional<PreparedAttachment> notice =
        tone == ReminderTone.FINAL
            ? formalNoticeAttachment(
                payment,
                contract,
                contact,
                outstanding,
                daysOverdue,
                languageTag,
                teamId,
                actorUserId)
            : Optional.empty();
    List<EmailAttachment> attachments = notice.map(n -> List.of(n.attachment())).orElse(List.of());

    return new TransactionTemplate(transactionManager)
        .execute(
            status ->
                persistReminder(
                    payment,
                    teamId,
                    type,
                    stepOffsetDays,
                    notes,
                    actorUserId,
                    actorName,
                    contact,
                    email,
                    daysOverdue,
                    outstanding,
                    currency,
                    tone,
                    languageTag,
                    variables,
                    attachments,
                    notice));
  }

  /** The transactional tail of a send: outbox entry, filed document, reminder row, audit. */
  private PaymentReminderResponse persistReminder(
      Payment payment,
      UUID teamId,
      ReminderType type,
      Optional<Integer> stepOffsetDays,
      Optional<String> notes,
      UUID actorUserId,
      Optional<String> actorName,
      Contact contact,
      String email,
      int daysOverdue,
      BigDecimal outstanding,
      String currency,
      ReminderTone tone,
      String languageTag,
      Map<String, Object> variables,
      List<EmailAttachment> attachments,
      Optional<PreparedAttachment> notice) {
    notice.ifPresent(
        n -> {
          documentRepository.save(n.document());
          metricsService.incrementCounter("payment.formal_notice.generated.total");
        });

    List<Notification> created =
        notificationService.send(
            SendNotificationRequest.builder()
                .teamId(Optional.of(teamId))
                .notificationType(PAYMENT_REMINDER)
                .recipientContactId(Optional.of(contact.getId()))
                .recipientEmail(Optional.of(email))
                .templateName(TEMPLATE_NAME)
                .templateVariables(variables)
                .languageTag(Optional.of(languageTag))
                .urgency(
                    tone == ReminderTone.FRIENDLY
                        ? NotificationUrgency.NORMAL
                        : NotificationUrgency.URGENT)
                .createdBy(actorUserId)
                .attachments(attachments)
                .build());
    Optional<Notification> emailNotification =
        created.stream().filter(n -> n.getChannel() == NotificationChannel.EMAIL).findFirst();

    PaymentReminder reminder =
        PaymentReminder.builder()
            .identifier(Optional.of(newPaymentReminderId()))
            .teamId(teamId)
            .paymentId(payment.getId())
            .contactId(Optional.of(contact.getId()))
            .reminderType(type)
            .channel(NotificationChannel.EMAIL)
            .recipientEmail(Optional.of(email))
            .daysOverdue(daysOverdue)
            .outstandingAmount(MoneyAmount.of(outstanding, currency))
            .notes(notes.filter(n -> !n.isBlank()))
            .stepOffsetDays(stepOffsetDays)
            .tone(Optional.of(tone))
            .notificationId(emailNotification.map(Notification::getId))
            .sentAt(clock.instant())
            .createdBy(actorUserId)
            .updatedBy(actorUserId)
            .build();
    reminderRepository.save(reminder);

    PaymentReminderResponse response =
        toResponse(
            reminder,
            actorName,
            emailNotification
                .map(n -> ReminderDeliveryStatus.from(n.getStatus()))
                .orElse(ReminderDeliveryStatus.UNKNOWN),
            Optional.empty());
    auditService.logCreate(teamId, "PAYMENT_REMINDER", reminder.getId(), actorUserId, response);
    metricsService.incrementCounter(
        "payment.reminder.sent.total", "type", type.name(), "channel", "EMAIL");
    log.info(
        "Sent {} payment reminder {} ({}) for payment {} to contact {} ({} days overdue)",
        type,
        reminder.getIdentifier().orElseThrow(),
        tone,
        payment.getIdentifier().map(Object::toString).orElse("?"),
        contact.getIdentifier().map(Object::toString).orElse("?"),
        daysOverdue);
    return response;
  }

  /** A rendered, uploaded formal notice waiting for its document row to be committed. */
  record PreparedAttachment(EmailAttachment attachment, Document document) {}

  /**
   * The template variables for a tenant reminder. Shared by the real send and the settings preview
   * so what a landlord previews is what a tenant receives.
   */
  private Map<String, Object> reminderVariables(
      String contactName,
      String propertyName,
      String teamName,
      BigDecimal amount,
      BigDecimal received,
      BigDecimal outstanding,
      String currency,
      LocalDate dueDate,
      int daysOverdue,
      ReminderTone tone,
      Optional<String> notes,
      String primaryUrl,
      Locale locale,
      Map<String, Object> paymentInstructions) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("contactName", contactName);
    variables.put("propertyName", propertyName);
    variables.put("teamName", teamName);
    variables.put("amount", CurrencyUtils.formatCurrency(amount, currency, locale));
    variables.put("outstanding", CurrencyUtils.formatCurrency(outstanding, currency, locale));
    variables.put("received", CurrencyUtils.formatCurrency(received, currency, locale));
    variables.put("hasPartialPayment", received.compareTo(ZERO) > 0);
    variables.put(
        "dueDate",
        dueDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)));
    variables.put("daysOverdue", daysOverdue);
    variables.put("isOverdue", daysOverdue > 0);
    variables.put("tone", tone.name());
    variables.put("isFinal", tone == ReminderTone.FINAL);
    variables.put("notes", notes.filter(n -> !n.isBlank()).orElse(""));
    variables.put("baseUrl", appProperties.email().baseUrl());
    variables.put("primaryUrl", primaryUrl);
    variables.putAll(paymentInstructions);
    return variables;
  }

  /**
   * Renders one ladder step against a worked example so a landlord can read the email before
   * switching the step on. Nothing is sent, stored or addressed to a real tenant.
   */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ReminderPreviewResponse previewReminder(
      ReminderTone tone, int offsetDays, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Team team = teamRepository.getById(teamId);
    TeamPreferences preferences = teamPreferencesRepository.getByTeamId(teamId);
    String currency = preferences.getDefaultCurrency();
    String languageTag =
        DocumentLanguages.firstSupportedOrDefault(List.of(preferences.getDefaultLanguage()));
    Locale locale = Locale.forLanguageTag(languageTag);
    LocalDate dueDate = LocalDate.now(clock).minusDays(offsetDays);
    int daysOverdue = Math.max(0, offsetDays);

    BigDecimal amount = SAMPLE_RENT;
    Map<String, Object> instructions = new HashMap<>();
    instructions.put("hasInstructions", true);
    instructions.put("iban", SAMPLE_IBAN);
    instructions.put("accountHolderName", team.getName());
    instructions.put("paymentReference", SAMPLE_REFERENCE);

    Map<String, Object> variables =
        reminderVariables(
            SAMPLE_TENANT_NAME,
            SAMPLE_PROPERTY,
            team.getName(),
            amount,
            ZERO,
            amount,
            currency,
            dueDate,
            daysOverdue,
            tone,
            Optional.empty(),
            appProperties.email().baseUrl() + "/payments",
            locale,
            instructions);

    RenderedContent rendered =
        notificationService.renderPreview(
            NotificationChannel.EMAIL, TEMPLATE_NAME, variables, locale);
    return new ReminderPreviewResponse(
        rendered.subject().orElse(""), rendered.body(), languageTag, SAMPLE_TENANT_NAME);
  }

  /**
   * Renders the formal notice PDF and uploads it; the document row is saved with the reminder so a
   * failed send leaves no dangling metadata (the S3 object itself is cheap to orphan). Skipped with
   * a warning when no PDF engine is deployed.
   */
  private Optional<PreparedAttachment> formalNoticeAttachment(
      Payment payment,
      Contract contract,
      Contact contact,
      BigDecimal outstanding,
      int daysOverdue,
      String languageTag,
      UUID teamId,
      UUID actorUserId) {
    Optional<TenantNoticeDocumentService> renderer =
        Optional.ofNullable(noticeDocumentService.getIfAvailable());
    if (renderer.isEmpty()) {
      log.warn("No TenantNoticeDocumentService available; sending FINAL reminder without PDF");
      return Optional.empty();
    }
    byte[] pdf =
        renderer
            .get()
            .renderFormalNotice(
                new TenantNoticeDocumentService.FormalNoticeData(
                    payment, contract, contact, outstanding, daysOverdue, languageTag));
    String paymentSid = payment.getIdentifier().map(Object::toString).orElse("payment");
    String fileName = "formal-notice-" + paymentSid + "-" + languageTag + ".pdf";
    Sid contractSid = contract.getIdentifier().orElseThrow();
    Sid teamSid = teamRepository.getById(teamId).getIdentifier().orElseThrow();
    String fileKey =
        s3StorageService.uploadFile(pdf, PDF_MIME, teamSid, CONTRACT_ENTITY, contractSid, fileName);
    Document document =
        Document.builder()
            .teamId(teamId)
            .entityType(CONTRACT_ENTITY)
            .entityId(contract.getId())
            .fileKey(fileKey)
            .fileName(fileName)
            .fileSize((long) pdf.length)
            .mimeType(PDF_MIME)
            .title(Optional.of("Formal notice - payment " + paymentSid + " (" + languageTag + ")"))
            .notes(Optional.empty())
            .uploadedBy(actorUserId)
            .build();
    return Optional.of(
        new PreparedAttachment(new EmailAttachment(fileName, PDF_MIME, fileKey), document));
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
      PaymentReminder reminder,
      Optional<String> sentByName,
      ReminderDeliveryStatus deliveryStatus,
      Optional<String> deliveryError) {
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
        reminder.getStepOffsetDays(),
        reminder.getTone(),
        reminder.getSentAt(),
        deliveryStatus,
        deliveryError);
  }

  /** Only rule/validation messages are safe to echo back; anything else stays generic. */
  private static String errorMessage(Exception e) {
    boolean clientFacing =
        e instanceof BusinessRuleException
            || e instanceof BadRequestException
            || e instanceof NotFoundException;
    String message = e.getMessage();
    return !clientFacing || message == null || message.isBlank()
        ? "An unexpected error occurred"
        : message;
  }
}
