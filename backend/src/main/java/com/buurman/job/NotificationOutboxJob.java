package com.buurman.job;

import static com.buurman.domain.NotificationStatus.FAILED;
import static com.buurman.domain.NotificationStatus.QUEUED;
import static com.buurman.domain.NotificationStatus.SENT;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.buurman.config.models.NotificationOutboxProperties;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationOutbox;
import com.buurman.repository.NotificationOutboxRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.service.notification.NotificationChannelSender;
import com.buurman.service.notification.NotificationSendException;
import com.buurman.service.notification.NotificationSendRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@DisallowConcurrentExecution
@Slf4j
public class NotificationOutboxJob implements Job {

  private final NotificationOutboxRepository outboxRepository;
  private final NotificationRepository notificationRepository;
  private final Map<NotificationChannel, NotificationChannelSender> channelSenders;
  private final ObjectMapper objectMapper;
  private final int batchSize;

  public NotificationOutboxJob(
      NotificationOutboxRepository outboxRepository,
      NotificationRepository notificationRepository,
      List<NotificationChannelSender> senders,
      ObjectMapper objectMapper,
      NotificationOutboxProperties outboxProperties) {
    this.outboxRepository = outboxRepository;
    this.notificationRepository = notificationRepository;
    this.objectMapper = objectMapper;
    this.batchSize = outboxProperties.batchSize();
    this.channelSenders =
        senders.stream().collect(toMap(NotificationChannelSender::getChannel, identity()));
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      List<NotificationOutbox> pending = outboxRepository.findPendingBatch(batchSize);

      if (pending.isEmpty()) {
        return;
      }

      log.info("Processing {} outbox entries", pending.size());

      for (NotificationOutbox entry : pending) {
        processEntry(entry);
      }
    } catch (Exception e) {
      log.error("Notification outbox job failed", e);
      throw new JobExecutionException("Notification outbox processing failed", e);
    }
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
      log.warn(
          "Failed to send notification {} via {}: {}",
          entry.getNotificationId(),
          entry.getChannel(),
          e.getMessage());
      outboxRepository.markFailed(entry.getId(), e.getMessage(), entry.getRetryCount());

      if (entry.getRetryCount() + 1 >= entry.getMaxRetries()) {
        notificationRepository.updateStatus(
            entry.getNotificationId(), FAILED, null, null, e.getMessage());
      } else {
        notificationRepository.updateStatus(
            entry.getNotificationId(), QUEUED, null, "retrying", null);
      }
    } catch (Exception e) {
      log.error(
          "Unexpected error processing outbox entry {}: {}", entry.getId(), e.getMessage(), e);
      outboxRepository.markFailed(entry.getId(), e.getMessage(), entry.getRetryCount());
    }
  }
}
