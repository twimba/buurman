package com.buurman.service.backoffice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Record2;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Notification;
import com.buurman.domain.Team;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeNotificationResponse;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeNotificationService {

  private final NotificationRepository notificationRepository;
  private final TeamRepository teamRepository;
  private final NotificationService notificationService;

  @Transactional(readOnly = true)
  public PageResponse<BackofficeNotificationResponse> listNotifications(
      PageRequest pageRequest,
      @Nullable String teamIdentifier,
      @Nullable String type,
      @Nullable String channel,
      @Nullable String status,
      @Nullable String recipientEmail,
      @Nullable LocalDateTime dateFrom,
      @Nullable LocalDateTime dateTo) {

    UUID teamId = null;
    if (teamIdentifier != null && !teamIdentifier.isBlank()) {
      teamId =
          teamRepository
              .findByIdentifierForBackoffice(teamIdentifier)
              .map(Team::getId)
              .orElse(null);
    }

    PaginatedResult<Notification> result =
        notificationRepository.findAllPaginatedUnscoped(
            teamId, type, channel, status, recipientEmail, dateFrom, dateTo, pageRequest);

    List<BackofficeNotificationResponse> responses =
        result.items().stream().map(this::toResponse).toList();

    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @Transactional(readOnly = true)
  public BackofficeNotificationResponse getNotification(String identifier) {
    Notification notification = notificationRepository.getByIdentifierUnscoped(identifier);
    return toResponse(notification);
  }

  @Transactional
  public BackofficeNotificationResponse resendNotification(
      String identifier, BackofficePrincipal principal) {
    Notification original = notificationRepository.getByIdentifierUnscoped(identifier);

    Notification resent =
        notificationService.resend(original.getTeamId(), original.getIdentifier(), null);

    log.info(
        "Backoffice user {} resent notification {} (type={}, channel={})",
        principal.getEmail(),
        identifier,
        original.getNotificationType(),
        original.getChannel());

    return toResponse(resent);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getStats() {
    long totalCount = notificationRepository.countAll();
    List<Record2<String, Integer>> statusCounts = notificationRepository.countGroupedByStatus();
    List<Record2<String, Integer>> channelCounts = notificationRepository.countGroupedByChannel();

    long pendingCount = 0, deliveredCount = 0, failedCount = 0;
    for (Record2<String, Integer> record : statusCounts) {
      String s = record.value1();
      int count = record.value2();
      switch (s) {
        case "PENDING", "QUEUED" -> pendingCount += count;
        case "DELIVERED" -> deliveredCount = count;
        case "FAILED", "BOUNCED", "REJECTED" -> failedCount += count;
      }
    }

    Map<String, Long> byChannel = new HashMap<>();
    for (Record2<String, Integer> record : channelCounts) {
      byChannel.put(record.value1(), (long) record.value2());
    }

    Map<String, Object> stats = new HashMap<>();
    stats.put("totalNotifications", totalCount);
    stats.put("pendingNotifications", pendingCount);
    stats.put("deliveredNotifications", deliveredCount);
    stats.put("failedNotifications", failedCount);
    stats.put("notificationsByChannel", byChannel);
    return stats;
  }

  private BackofficeNotificationResponse toResponse(Notification notification) {
    String teamIdentifier = null;
    String teamName = null;
    if (notification.getTeamId() != null) {
      Team team = teamRepository.findById(notification.getTeamId()).orElse(null);
      if (team != null) {
        teamIdentifier = team.getIdentifier();
        teamName = team.getName();
      }
    }

    String resentFromIdentifier = null;
    if (notification.getResentFromId() != null) {
      resentFromIdentifier =
          notificationRepository
              .findByIdentifierUnscoped(notification.getResentFromId().toString())
              .map(Notification::getIdentifier)
              .orElse(null);
    }

    return new BackofficeNotificationResponse(
        notification.getIdentifier(),
        Optional.ofNullable(teamIdentifier),
        Optional.ofNullable(teamName),
        notification.getNotificationType().name(),
        notification.getChannel().name(),
        Optional.ofNullable(notification.getSubject()),
        Optional.ofNullable(notification.getBody()),
        Optional.ofNullable(notification.getRecipientEmail()),
        Optional.ofNullable(notification.getRecipientPhone()),
        notification.getStatus().name(),
        Optional.ofNullable(notification.getProviderStatus()),
        Optional.ofNullable(notification.getProviderError()),
        notification.getOpenCount(),
        notification.getClickCount(),
        Optional.ofNullable(notification.getFirstOpenedAt()),
        Optional.ofNullable(notification.getFirstClickedAt()),
        Optional.ofNullable(resentFromIdentifier),
        Optional.ofNullable(notification.getResendReason()),
        notification.getCreatedAt(),
        Optional.ofNullable(notification.getStatusUpdatedAt()));
  }
}
