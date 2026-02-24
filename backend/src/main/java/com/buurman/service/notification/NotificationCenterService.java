package com.buurman.service.notification;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jooq.Record2;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Notification;
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
  public NotificationResponse getNotification(UserPrincipal principal, String identifier) {
    Notification notification =
        notificationRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    return toResponse(notification);
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public NotificationStatsResponse getStats(UserPrincipal principal) {
    List<Record2<String, Integer>> statusCounts =
        notificationRepository.countByTeamIdGroupedByStatus(principal.requireTeamId());
    List<Record2<String, Integer>> channelCounts =
        notificationRepository.countByTeamIdGroupedByChannel(principal.requireTeamId());
    long totalCount = notificationRepository.countByTeamId(principal.requireTeamId());

    long pendingCount = 0, sentCount = 0, deliveredCount = 0, failedCount = 0;
    for (Record2<String, Integer> record : statusCounts) {
      String s = record.value1();
      int count = record.value2();
      switch (s) {
        case "PENDING", "QUEUED" -> pendingCount += count;
        case "SENT" -> sentCount = count;
        case "DELIVERED" -> deliveredCount = count;
        case "FAILED", "BOUNCED", "REJECTED" -> failedCount += count;
      }
    }

    Map<String, Long> byChannel = new HashMap<>();
    for (Record2<String, Integer> record : channelCounts) {
      byChannel.put(record.value1(), (long) record.value2());
    }

    return new NotificationStatsResponse(
        totalCount, pendingCount, sentCount, deliveredCount, failedCount, byChannel);
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  @Transactional
  public NotificationResponse resendNotification(UserPrincipal principal, String identifier) {
    Notification resent =
        notificationService.resend(principal.requireTeamId(), identifier, principal.getUserId());
    return toResponse(resent);
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public NotificationResponse refreshNotificationStatus(
      UserPrincipal principal, String identifier) {
    Notification notification =
        notificationRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    Notification updated = deliveryStatusLookupService.refreshStatus(notification);
    return toResponse(updated);
  }

  private NotificationResponse toResponse(Notification notification) {
    String resentFromIdentifier =
        notification
            .getResentFromId()
            .flatMap(
                resentId ->
                    notificationRepository.findByIdAndTeamId(
                        resentId, notification.getTeamId().orElse(null)))
            .map(Notification::getIdentifier)
            .orElse(null);

    return new NotificationResponse(
        notification.getIdentifier(),
        notification.getNotificationType().name(),
        notification.getChannel().name(),
        notification.getSubject(),
        Optional.ofNullable(notification.getBody()),
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
