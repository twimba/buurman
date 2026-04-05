package com.buurman.job;

import static com.buurman.domain.NotificationChannel.EMAIL;
import static com.buurman.domain.NotificationChannel.SMS;
import static com.buurman.domain.NotificationStatus.FAILED;
import static com.buurman.domain.NotificationStatus.QUEUED;
import static com.buurman.domain.NotificationStatus.SENT;
import static com.buurman.domain.NotificationUrgency.URGENT;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.config.models.AppProperties;
import com.buurman.config.models.NotificationConsolidationProperties;
import com.buurman.config.models.NotificationOutboxProperties;
import com.buurman.domain.DigestItem;
import com.buurman.domain.Notification;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationOutbox;
import com.buurman.domain.NotificationType;
import com.buurman.domain.TeamPreferences;
import com.buurman.domain.UserPreferences;
import com.buurman.repository.NotificationOutboxRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.service.notification.DigestRenderer;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendException;
import com.buurman.service.notification.NotificationSendRequest;
import com.buurman.service.notification.RenderedContent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@DisallowConcurrentExecution
@Slf4j
public class NotificationOutboxJob implements Job {

  private final NotificationOutboxRepository outboxRepository;
  private final NotificationRepository notificationRepository;
  private final UserPreferencesRepository userPreferencesRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final Map<NotificationChannel, NotificationChannelSender> channelSenders;
  private final ObjectMapper objectMapper;
  private final DigestRenderer digestRenderer;
  private final NotificationConsolidationProperties consolidationProperties;
  private final String baseUrl;
  private final int batchSize;

  public NotificationOutboxJob(
      NotificationOutboxRepository outboxRepository,
      NotificationRepository notificationRepository,
      UserPreferencesRepository userPreferencesRepository,
      TeamPreferencesRepository teamPreferencesRepository,
      List<NotificationChannelSender> senders,
      ObjectMapper objectMapper,
      NotificationOutboxProperties outboxProperties,
      DigestRenderer digestRenderer,
      NotificationConsolidationProperties consolidationProperties,
      AppProperties appProperties) {
    this.outboxRepository = outboxRepository;
    this.notificationRepository = notificationRepository;
    this.userPreferencesRepository = userPreferencesRepository;
    this.teamPreferencesRepository = teamPreferencesRepository;
    this.objectMapper = objectMapper;
    this.batchSize = outboxProperties.batchSize();
    this.channelSenders =
        senders.stream().collect(toMap(NotificationChannelSender::getChannel, identity()));
    this.digestRenderer = digestRenderer;
    this.consolidationProperties = consolidationProperties;
    this.baseUrl = appProperties.email().baseUrl();
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      List<NotificationOutbox> pending = outboxRepository.findPendingBatch(batchSize);

      if (pending.isEmpty()) {
        return;
      }

      context.put("itemsProcessed", pending.size());
      log.info("Processing {} outbox entries", pending.size());

      if (isConsolidationEnabled()) {
        consolidateAndProcess(pending);
      } else {
        for (NotificationOutbox entry : pending) {
          processEntry(entry);
        }
      }
    } catch (Exception e) {
      log.error("Notification outbox job failed", e);
      throw new JobExecutionException("Notification outbox processing failed", e);
    }
  }

  private boolean isConsolidationEnabled() {
    return consolidationProperties.enabled();
  }

  private void consolidateAndProcess(List<NotificationOutbox> pending) {
    List<NotificationOutbox> individualEntries = new ArrayList<>();
    List<NotificationOutbox> consolidatableEntries = new ArrayList<>();

    for (NotificationOutbox entry : pending) {
      if (shouldProcessIndividually(entry)) {
        individualEntries.add(entry);
      } else {
        consolidatableEntries.add(entry);
      }
    }

    // Process individual entries as before
    for (NotificationOutbox entry : individualEntries) {
      processEntry(entry);
    }

    // Group consolidatable entries by consolidation key
    Map<String, List<NotificationOutbox>> groups =
        consolidatableEntries.stream().collect(Collectors.groupingBy(this::consolidationKey));

    for (Map.Entry<String, List<NotificationOutbox>> group : groups.entrySet()) {
      List<NotificationOutbox> entries = group.getValue();
      if (shouldConsolidate(entries)) {
        processConsolidatedGroup(entries);
      } else {
        // Below threshold, send individually
        for (NotificationOutbox entry : entries) {
          processEntry(entry);
        }
      }
    }
  }

  private boolean shouldProcessIndividually(NotificationOutbox entry) {
    // Urgent notifications always sent individually
    if (entry.getUrgency() == URGENT) {
      return true;
    }

    // Non-consolidatable types sent individually
    return entry.getNotificationType().map(type -> !type.isConsolidatable()).orElse(true);
  }

  private String consolidationKey(NotificationOutbox entry) {
    String recipient =
        entry
            .getRecipientUserId()
            .map(UUID::toString)
            .orElseGet(() -> entry.getRecipientEmail().orElse("unknown"));
    String teamId = entry.getTeamId().map(UUID::toString).orElse("no-team");
    String channel = entry.getChannel().name();
    String type = entry.getNotificationType().map(Enum::name).orElse("UNKNOWN");
    return recipient + "|" + teamId + "|" + channel + "|" + type;
  }

  private boolean shouldConsolidate(List<NotificationOutbox> entries) {
    NotificationChannel channel = entries.getFirst().getChannel();
    if (channel == EMAIL) {
      return consolidationProperties.email().enabled()
          && entries.size() >= consolidationProperties.email().minGroupSize();
    }
    if (channel == SMS) {
      return consolidationProperties.sms().enabled()
          && entries.size() > consolidationProperties.sms().threshold();
    }
    return false;
  }

  private void processConsolidatedGroup(List<NotificationOutbox> entries) {
    NotificationOutbox first = entries.getFirst();
    NotificationChannel channel = first.getChannel();
    Optional<NotificationType> notificationTypeOpt = first.getNotificationType();

    if (notificationTypeOpt.isEmpty()) {
      log.warn("Missing notification type for consolidation, processing individually");
      for (NotificationOutbox entry : entries) {
        processEntry(entry);
      }
      return;
    }

    NotificationType notificationType = notificationTypeOpt.get();

    // Load notifications to get contentVariables for digest rendering
    List<DigestItem> digestItems = new ArrayList<>();
    for (NotificationOutbox entry : entries) {
      notificationRepository
          .findByIdAndTeamId(entry.getNotificationId(), entry.getTeamId().orElse(null))
          .ifPresent(
              notification ->
                  notification
                      .getContentVariables()
                      .ifPresent(
                          vars ->
                              digestItems.add(
                                  digestRenderer.buildDigestItem(
                                      notificationType, vars, baseUrl))));
    }

    if (digestItems.isEmpty()) {
      log.warn("No digest items could be built for consolidation group, processing individually");
      for (NotificationOutbox entry : entries) {
        processEntry(entry);
      }
      return;
    }

    // Assign consolidation group ID to all entries in this group
    UUID groupId = UUID.randomUUID();
    List<UUID> entryIds = entries.stream().map(NotificationOutbox::getId).toList();
    outboxRepository.setConsolidationGroupId(entryIds, groupId);

    String recipientName = resolveRecipientName(entries);

    try {
      if (channel == EMAIL) {
        processConsolidatedEmail(entries, notificationType, digestItems, recipientName, groupId);
      } else if (channel == SMS) {
        processConsolidatedSms(entries, notificationType, digestItems, groupId);
      }
    } catch (Exception e) {
      log.error(
          "Failed to send consolidated notification for group {}, falling back to individual sends",
          groupId,
          e);
      for (NotificationOutbox entry : entries) {
        processEntry(entry);
      }
    }
  }

  private void processConsolidatedEmail(
      List<NotificationOutbox> entries,
      NotificationType notificationType,
      List<DigestItem> digestItems,
      String recipientName,
      UUID groupId) {

    NotificationOutbox first = entries.getFirst();
    NotificationChannelSender sender = channelSenders.get(EMAIL);
    if (sender == null) {
      log.error("No sender registered for EMAIL channel");
      return;
    }

    // Resolve recipient locale: user preference → team default → English
    Optional<String> userLang =
        first
            .getRecipientUserId()
            .flatMap(userPreferencesRepository::findByUserId)
            .map(UserPreferences::getLanguage);
    Locale recipientLocale =
        userLang
            .map(Locale::forLanguageTag)
            .orElseGet(
                () ->
                    first
                        .getTeamId()
                        .flatMap(teamPreferencesRepository::findByTeamId)
                        .map(tp -> Locale.forLanguageTag(tp.getDefaultLanguage()))
                        .orElse(Locale.ENGLISH));

    // Render digest email via the Thymeleaf template
    Map<String, Object> templateVars =
        digestRenderer.buildEmailDigestVariables(
            notificationType, digestItems, recipientName, baseUrl);
    RenderedContent content = sender.render("notification-digest", templateVars, recipientLocale);

    String subject = digestRenderer.buildEmailDigestSubject(notificationType, digestItems);

    NotificationSendRequest sendRequest =
        new NotificationSendRequest(
            Optional.empty(),
            first.getRecipientEmail(),
            Optional.empty(),
            Optional.of(subject),
            content.body(),
            Optional.empty(),
            Optional.empty());

    try {
      // Mark all original entries as processing
      for (NotificationOutbox entry : entries) {
        outboxRepository.markProcessing(entry.getId());
      }

      String providerMessageId = sender.send(sendRequest);

      // Mark all as sent
      List<UUID> entryIds = entries.stream().map(NotificationOutbox::getId).toList();
      outboxRepository.markSentBatch(entryIds);

      // Update each notification's status
      for (NotificationOutbox entry : entries) {
        notificationRepository.updateStatus(
            entry.getNotificationId(), SENT, providerMessageId, "sent_consolidated", null);
      }

      log.info(
          "Sent consolidated EMAIL digest ({} items, type={}, groupId={})",
          digestItems.size(),
          notificationType,
          groupId);

    } catch (NotificationSendException e) {
      handleConsolidatedSendFailure(entries, e);
    }
  }

  private void processConsolidatedSms(
      List<NotificationOutbox> entries,
      NotificationType notificationType,
      List<DigestItem> digestItems,
      UUID groupId) {

    NotificationOutbox first = entries.getFirst();
    NotificationChannelSender sender = channelSenders.get(SMS);
    if (sender == null) {
      log.error("No sender registered for SMS channel");
      return;
    }

    String smsBody = digestRenderer.buildSmsSummary(notificationType, digestItems, baseUrl);

    // Retrieve recipient phone from the first entry's payload
    Optional<String> recipientPhone;
    try {
      NotificationSendRequest originalPayload =
          objectMapper.readValue(first.getPayload(), NotificationSendRequest.class);
      recipientPhone = originalPayload.recipientPhone();
    } catch (Exception e) {
      log.warn("Failed to deserialize payload for SMS consolidation, processing individually", e);
      for (NotificationOutbox entry : entries) {
        processEntry(entry);
      }
      return;
    }

    NotificationSendRequest sendRequest =
        new NotificationSendRequest(
            Optional.empty(),
            Optional.empty(),
            recipientPhone,
            Optional.empty(),
            smsBody,
            Optional.empty(),
            Optional.empty());

    try {
      for (NotificationOutbox entry : entries) {
        outboxRepository.markProcessing(entry.getId());
      }

      String providerMessageId = sender.send(sendRequest);

      List<UUID> entryIds = entries.stream().map(NotificationOutbox::getId).toList();
      outboxRepository.markSentBatch(entryIds);

      for (NotificationOutbox entry : entries) {
        notificationRepository.updateStatus(
            entry.getNotificationId(), SENT, providerMessageId, "sent_consolidated", null);
      }

      log.info(
          "Sent consolidated SMS digest ({} items, type={}, groupId={})",
          digestItems.size(),
          notificationType,
          groupId);

    } catch (NotificationSendException e) {
      handleConsolidatedSendFailure(entries, e);
    }
  }

  private void handleConsolidatedSendFailure(
      List<NotificationOutbox> entries, NotificationSendException e) {
    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    log.warn("Failed to send consolidated digest: {}", errorMsg);

    for (NotificationOutbox entry : entries) {
      outboxRepository.markFailed(entry.getId(), errorMsg, entry.getRetryCount());
      if (entry.getRetryCount() + 1 >= entry.getMaxRetries()) {
        notificationRepository.updateStatus(
            entry.getNotificationId(), FAILED, null, null, errorMsg);
      } else {
        notificationRepository.updateStatus(
            entry.getNotificationId(), QUEUED, null, "retrying", null);
      }
    }
  }

  private String resolveRecipientName(List<NotificationOutbox> entries) {
    for (NotificationOutbox entry : entries) {
      Optional<Notification> notif =
          notificationRepository.findByIdAndTeamId(
              entry.getNotificationId(), entry.getTeamId().orElse(null));
      if (notif.isPresent()) {
        Optional<Map<String, Object>> vars = notif.get().getContentVariables();
        if (vars.isPresent()) {
          Object userName = vars.get().get("userName");
          if (userName != null) {
            return userName.toString();
          }
        }
      }
    }
    return "there";
  }

  private void processEntry(NotificationOutbox entry) {
    try {
      outboxRepository.markProcessing(entry.getId());

      NotificationChannelSender sender = channelSenders.get(entry.getChannel());
      if (sender == null) {
        String error = "No sender registered for channel: " + entry.getChannel();
        log.error(error);
        outboxRepository.markFailed(entry.getId(), error, entry.getRetryCount());
        notificationRepository.updateStatus(entry.getNotificationId(), FAILED, null, null, error);
        return;
      }

      NotificationSendRequest sendRequest =
          objectMapper.readValue(entry.getPayload(), NotificationSendRequest.class);

      String providerMessageId = sender.send(sendRequest);

      outboxRepository.markSent(entry.getId());
      notificationRepository.updateStatus(
          entry.getNotificationId(), SENT, providerMessageId, "sent", null);

      log.debug(
          "Successfully sent notification {} via {} (provider ID: {})",
          entry.getNotificationId(),
          entry.getChannel(),
          providerMessageId);

    } catch (NotificationSendException e) {
      String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      log.warn(
          "Failed to send notification {} via {}: {}",
          entry.getNotificationId(),
          entry.getChannel(),
          errorMsg);
      outboxRepository.markFailed(entry.getId(), errorMsg, entry.getRetryCount());

      if (entry.getRetryCount() + 1 >= entry.getMaxRetries()) {
        notificationRepository.updateStatus(
            entry.getNotificationId(), FAILED, null, null, errorMsg);
      } else {
        notificationRepository.updateStatus(
            entry.getNotificationId(), QUEUED, null, "retrying", null);
      }
    } catch (Exception e) {
      String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      log.error("Unexpected error processing outbox entry {}: {}", entry.getId(), errorMsg, e);
      outboxRepository.markFailed(entry.getId(), errorMsg, entry.getRetryCount());
    }
  }
}
