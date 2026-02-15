package com.buurman.service.backoffice;

import com.buurman.domain.Notification;
import com.buurman.domain.Team;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeNotificationResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.util.PaginationHelper.PaginatedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeNotificationService {

    private final NotificationRepository notificationRepository;
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<BackofficeNotificationResponse> listNotifications(
            PageRequest pageRequest, String teamIdentifier, String type, String channel,
            String status, String recipientEmail, LocalDateTime dateFrom, LocalDateTime dateTo) {

        UUID teamId = null;
        if (teamIdentifier != null && !teamIdentifier.isBlank()) {
            teamId = teamRepository.findByIdentifierForBackoffice(teamIdentifier)
                    .map(Team::getId)
                    .orElse(null);
        }

        PaginatedResult<Notification> result = notificationRepository.findAllPaginatedUnscoped(
                teamId, type, channel, status, recipientEmail, dateFrom, dateTo, pageRequest);

        List<BackofficeNotificationResponse> responses = result.items().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), result.totalElements());
    }

    @Transactional(readOnly = true)
    public BackofficeNotificationResponse getNotification(String identifier) {
        Notification notification = notificationRepository.findByIdentifierUnscoped(identifier)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        return toResponse(notification);
    }

    @Transactional
    public BackofficeNotificationResponse resendNotification(String identifier, BackofficePrincipal principal) {
        Notification original = notificationRepository.findByIdentifierUnscoped(identifier)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        Notification resent = notificationService.resend(
                original.getTeamId(), original.getIdentifier(), null);

        log.info("Backoffice user {} resent notification {} (type={}, channel={})",
                principal.getEmail(), identifier,
                original.getNotificationType(), original.getChannel());

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
            resentFromIdentifier = notificationRepository
                    .findByIdentifierUnscoped(notification.getResentFromId().toString())
                    .map(Notification::getIdentifier)
                    .orElse(null);
        }

        return new BackofficeNotificationResponse(
                notification.getIdentifier(),
                teamIdentifier,
                teamName,
                notification.getNotificationType().name(),
                notification.getChannel().name(),
                notification.getSubject(),
                notification.getBody(),
                notification.getRecipientEmail(),
                notification.getRecipientPhone(),
                notification.getStatus().name(),
                notification.getProviderStatus(),
                notification.getProviderError(),
                resentFromIdentifier,
                notification.getResendReason(),
                notification.getCreatedAt(),
                notification.getStatusUpdatedAt()
        );
    }
}
