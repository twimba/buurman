package com.buurman.service.notification;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.LabelCount;
import com.buurman.domain.Notification;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.NotificationResponse;
import com.buurman.dto.response.NotificationStatsResponse;
import com.buurman.repository.NotificationRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationCenterService {

  private final NotificationRepository notificationRepository;
  private final NotificationService notificationService;
  private final DeliveryStatusLookupService deliveryStatusLookupService;

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public PaginatedResult<NotificationResponse> getNotifications(
      UserPrincipal principal,
      @Nullable String type,
      @Nullable String channel,
      @Nullable String status,
      @Nullable String recipientEmail,
      @Nullable String dateFrom,
      @Nullable String dateTo,
      PageRequest pageRequest) {

    LocalDateTime from = parseDateTime(dateFrom);
    LocalDateTime to = parseDateTime(dateTo);

    PaginatedResult<Notification> result =
        notificationRepository.findAllByTeamIdPaginated(
            principal.requireTeamId(),
            type,
            channel,
            status,
            recipientEmail,
            from,
            to,
            pageRequest);

    List<NotificationResponse> responses = result.items().stream().map(this::toResponse).toList();

    return new PaginatedResult<>(responses, result.totalElements());
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public NotificationResponse getNotification(
      UserPrincipal principal, NotificationIdentifier identifier) {
    Notification notification =
        notificationRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return toResponse(notification);
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public NotificationStatsResponse getStats(UserPrincipal principal) {
    List<LabelCount> statusCounts =
        notificationRepository.countByTeamIdGroupedByStatus(principal.requireTeamId());
    List<LabelCount> channelCounts =
        notificationRepository.countByTeamIdGroupedByChannel(principal.requireTeamId());
    long totalCount = notificationRepository.countByTeamId(principal.requireTeamId());

    long pendingCount = 0, sentCount = 0, deliveredCount = 0, failedCount = 0, demoBlockedCount = 0;
    for (LabelCount record : statusCounts) {
      String s = record.label();
      int count = record.count();
      switch (s) {
        case "PENDING", "QUEUED" -> pendingCount += count;
        case "SENT" -> sentCount = count;
        case "DELIVERED" -> deliveredCount = count;
        case "FAILED", "BOUNCED", "REJECTED" -> failedCount += count;
        case "DEMO_BLOCKED" -> demoBlockedCount = count;
      }
    }

    Map<String, Long> byChannel = new HashMap<>();
    for (LabelCount record : channelCounts) {
      byChannel.put(record.label(), (long) record.count());
    }

    return new NotificationStatsResponse(
        totalCount,
        pendingCount,
        sentCount,
        deliveredCount,
        failedCount,
        demoBlockedCount,
        byChannel);
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  @Transactional
  public NotificationResponse resendNotification(
      UserPrincipal principal, NotificationIdentifier identifier) {
    Notification resent =
        notificationService.resend(principal.requireTeamId(), identifier, principal.getUserId());
    return toResponse(resent);
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public NotificationResponse refreshNotificationStatus(
      UserPrincipal principal, NotificationIdentifier identifier) {
    Notification notification =
        notificationRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    Notification updated = deliveryStatusLookupService.refreshStatus(notification);
    return toResponse(updated);
  }

  private NotificationResponse toResponse(Notification notification) {
    Sid resentFromIdentifier =
        notification
            .getResentFromId()
            .flatMap(
                resentId ->
                    notificationRepository.findByIdAndTeamId(
                        resentId, notification.getTeamId().orElse(null)))
            .flatMap(Notification::getIdentifier)
            .orElse(null);

    return new NotificationResponse(
        notification.getIdentifier().orElseThrow(),
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
        Optional.ofNullable(resentFromIdentifier),
        notification.getResendReason(),
        notification.getStatus() == NotificationStatus.DEMO_BLOCKED,
        notification.getCreatedAt(),
        notification.getStatusUpdatedAt());
  }

  private @Nullable LocalDateTime parseDateTime(@Nullable String dateTimeStr) {
    if (dateTimeStr == null || dateTimeStr.isBlank()) {
      return null;
    }
    try {
      return LocalDateTime.parse(dateTimeStr, ISO_DATE_TIME);
    } catch (Exception e) {
      try {
        return LocalDateTime.parse(dateTimeStr + "T00:00:00", ISO_DATE_TIME);
      } catch (Exception e2) {
        return null;
      }
    }
  }
}
