package com.buurman.service.backoffice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.LabelCount;
import com.buurman.domain.Notification;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.domain.identifier.TeamIdentifier;
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
      @Nullable TeamIdentifier teamIdentifier,
      @Nullable String type,
      @Nullable String channel,
      @Nullable String status,
      @Nullable String recipientEmail,
      @Nullable LocalDateTime dateFrom,
      @Nullable LocalDateTime dateTo) {

    UUID teamId = null;
    if (teamIdentifier != null) {
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
  public BackofficeNotificationResponse getNotification(NotificationIdentifier identifier) {
    Notification notification = notificationRepository.getByIdentifierUnscoped(identifier);
    return toResponse(notification);
  }

  @Transactional
  public BackofficeNotificationResponse resendNotification(
      NotificationIdentifier identifier, BackofficePrincipal principal) {
    Notification original = notificationRepository.getByIdentifierUnscoped(identifier);

    Notification resent =
        notificationService.resend(original.getTeamId().orElse(null), identifier, null);

    log.info(
        "Backoffice user {} resent notification {} (type={}, channel={})",
        principal.getEmail().orElse("unknown"),
        identifier,
        original.getNotificationType(),
        original.getChannel());

    return toResponse(resent);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getStats() {
    long totalCount = notificationRepository.countAll();
    List<LabelCount> statusCounts = notificationRepository.countGroupedByStatus();
    List<LabelCount> channelCounts = notificationRepository.countGroupedByChannel();

    // Per-state counts power the hero bar; only states present in the data appear here, the UI
    // defaults the rest to 0 so every lifecycle state always shows.
    Map<String, Long> byStatus = new HashMap<>();
    for (LabelCount record : statusCounts) {
      byStatus.put(record.label(), (long) record.count());
    }

    Map<String, Long> byChannel = new HashMap<>();
    for (LabelCount record : channelCounts) {
      byChannel.put(record.label(), (long) record.count());
    }

    Map<String, Object> stats = new HashMap<>();
    stats.put("total", totalCount);
    stats.put("byStatus", byStatus);
    stats.put("byChannel", byChannel);
    return stats;
  }

  private BackofficeNotificationResponse toResponse(Notification notification) {
    Sid teamIdentifier = null;
    String teamName = null;
    if (notification.getTeamId().isPresent()) {
      Team team = teamRepository.findById(notification.getTeamId().get()).orElse(null);
      if (team != null) {
        teamIdentifier = team.getIdentifier().orElseThrow();
        teamName = team.getName();
      }
    }

    Optional<Sid> resentFromIdentifier =
        notification
            .getResentFromId()
            .flatMap(id -> notificationRepository.findByIdAndTeamId(id, null))
            .flatMap(Notification::getIdentifier);

    return new BackofficeNotificationResponse(
        notification.getIdentifier().orElseThrow(),
        Optional.ofNullable(teamIdentifier),
        Optional.ofNullable(teamName),
        notification.getNotificationType().name(),
        notification.getChannel().name(),
        notification.getSubject(),
        Optional.of(notification.getBody()),
        notification.getRecipientEmail(),
        notification.getRecipientPhone(),
        notification.getStatus().name(),
        notification.getProviderStatus(),
        notification.getProviderError(),
        notification.getOpenCount(),
        notification.getClickCount(),
        notification.getFirstOpenedAt(),
        notification.getFirstClickedAt(),
        resentFromIdentifier,
        notification.getResendReason(),
        notification.getStatus() == NotificationStatus.DEMO_BLOCKED,
        notification.getCreatedAt(),
        notification.getStatusUpdatedAt());
  }
}
