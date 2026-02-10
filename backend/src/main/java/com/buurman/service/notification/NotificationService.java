package com.buurman.service.notification;

import com.buurman.domain.*;
import com.buurman.repository.NotificationOutboxRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.repository.UserTeamNotificationPreferencesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserTeamNotificationPreferencesRepository teamNotifPrefsRepository;
    private final Map<NotificationChannel, NotificationChannelSender> channelSenders;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationOutboxRepository outboxRepository,
                               UserPreferencesRepository userPreferencesRepository,
                               UserTeamNotificationPreferencesRepository teamNotifPrefsRepository,
                               List<NotificationChannelSender> senders,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.outboxRepository = outboxRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.teamNotifPrefsRepository = teamNotifPrefsRepository;
        this.objectMapper = objectMapper;

        this.channelSenders = new HashMap<>();
        for (NotificationChannelSender sender : senders) {
            this.channelSenders.put(sender.getChannel(), sender);
        }
    }

    @Transactional
    public List<Notification> send(SendNotificationRequest request) {
        List<NotificationChannel> channels = resolveChannels(request);
        List<Notification> notifications = new ArrayList<>();

        for (NotificationChannel channel : channels) {
            if (!canSendViaChannel(channel, request)) {
                log.debug("Skipping channel {} for notification type {} - missing recipient info",
                        channel, request.notificationType());
                continue;
            }

            NotificationChannelSender sender = channelSenders.get(channel);
            if (sender == null) {
                log.warn("No sender registered for channel: {}", channel);
                continue;
            }

            RenderedContent content = sender.render(request.templateName(), request.templateVariables());

            Notification notification = new Notification();
            notification.setTeamId(request.teamId());
            notification.setNotificationType(request.notificationType());
            notification.setSubject(content.subject());
            notification.setRecipientEmail(request.recipientEmail());
            notification.setRecipientPhone(request.recipientPhone());
            notification.setRecipientUserId(request.recipientUserId());
            notification.setRecipientTenantId(request.recipientTenantId());
            notification.setChannel(channel);
            notification.setContentTemplate(request.templateName());
            notification.setContentVariables(request.templateVariables());
            notification.setStatus(NotificationStatus.PENDING);
            notification.setCreatedBy(request.createdBy());

            notification = notificationRepository.save(notification);

            NotificationOutbox outbox = new NotificationOutbox();
            outbox.setNotificationId(notification.getId());
            outbox.setChannel(channel);
            outbox.setMaxRetries(3);

            NotificationSendRequest sendRequest = new NotificationSendRequest(
                    notification.getId(),
                    request.recipientEmail(),
                    request.recipientPhone(),
                    content.subject(),
                    content.body(),
                    null,
                    null
            );

            try {
                outbox.setPayload(objectMapper.writeValueAsString(sendRequest));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize send request", e);
            }

            outboxRepository.save(outbox);
            notifications.add(notification);
        }

        return notifications;
    }

    @Transactional
    public Notification resend(UUID teamId, String notificationIdentifier, UUID userId) {
        Notification original = notificationRepository.findByIdentifierAndTeamId(notificationIdentifier, teamId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        NotificationChannelSender sender = channelSenders.get(original.getChannel());
        if (sender == null) {
            throw new RuntimeException("No sender for channel: " + original.getChannel());
        }

        RenderedContent content = sender.render(original.getContentTemplate(), original.getContentVariables());

        Notification resent = new Notification();
        resent.setTeamId(original.getTeamId());
        resent.setNotificationType(original.getNotificationType());
        resent.setSubject(content.subject());
        resent.setRecipientEmail(original.getRecipientEmail());
        resent.setRecipientPhone(original.getRecipientPhone());
        resent.setRecipientUserId(original.getRecipientUserId());
        resent.setRecipientTenantId(original.getRecipientTenantId());
        resent.setChannel(original.getChannel());
        resent.setContentTemplate(original.getContentTemplate());
        resent.setContentVariables(original.getContentVariables());
        resent.setStatus(NotificationStatus.PENDING);
        resent.setResentFromId(original.getId());
        resent.setResendReason("Resent by user");
        resent.setCreatedBy(userId);

        resent = notificationRepository.save(resent);

        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setNotificationId(resent.getId());
        outbox.setChannel(resent.getChannel());
        outbox.setMaxRetries(3);

        NotificationSendRequest sendRequest = new NotificationSendRequest(
                resent.getId(),
                resent.getRecipientEmail(),
                resent.getRecipientPhone(),
                content.subject(),
                content.body(),
                null,
                null
        );

        try {
            outbox.setPayload(objectMapper.writeValueAsString(sendRequest));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize send request", e);
        }

        outboxRepository.save(outbox);
        return resent;
    }

    private List<NotificationChannel> resolveChannels(SendNotificationRequest request) {
        if (request.recipientUserId() == null) {
            return List.of(NotificationChannel.EMAIL);
        }

        UserPreferences globalPrefs = userPreferencesRepository.findByUserId(request.recipientUserId())
                .orElseGet(UserPreferences::new);

        if (request.teamId() == null) {
            List<NotificationChannel> channels = new ArrayList<>();
            if (globalPrefs.isEmailNotifications()) {
                channels.add(NotificationChannel.EMAIL);
            }
            if (globalPrefs.isSmsNotifications()) {
                channels.add(NotificationChannel.SMS);
            }
            return channels.isEmpty() ? List.of(NotificationChannel.EMAIL) : channels;
        }

        Optional<UserTeamNotificationPreferences> teamPrefsOpt = teamNotifPrefsRepository
                .findByUserIdAndTeamId(request.recipientUserId(), request.teamId());

        if (teamPrefsOpt.isEmpty()) {
            List<NotificationChannel> channels = new ArrayList<>();
            if (globalPrefs.isEmailNotifications()) {
                channels.add(NotificationChannel.EMAIL);
            }
            if (globalPrefs.isSmsNotifications()) {
                channels.add(NotificationChannel.SMS);
            }
            return channels.isEmpty() ? List.of(NotificationChannel.EMAIL) : channels;
        }

        UserTeamNotificationPreferences teamPrefs = teamPrefsOpt.get();

        if (!isNotificationTypeEnabled(request.notificationType(), teamPrefs)) {
            return List.of();
        }

        String preferredChannels = teamPrefs.getPreferredChannels();
        if (preferredChannels == null || preferredChannels.isBlank()) {
            preferredChannels = "EMAIL";
        }

        List<NotificationChannel> channels = new ArrayList<>();
        for (String ch : preferredChannels.split(",")) {
            String trimmed = ch.trim();
            if ("EMAIL".equals(trimmed) && globalPrefs.isEmailNotifications()) {
                channels.add(NotificationChannel.EMAIL);
            } else if ("SMS".equals(trimmed) && globalPrefs.isSmsNotifications()) {
                channels.add(NotificationChannel.SMS);
            }
        }

        return channels.isEmpty() ? List.of(NotificationChannel.EMAIL) : channels;
    }

    private boolean isNotificationTypeEnabled(NotificationType type, UserTeamNotificationPreferences prefs) {
        return switch (type) {
            case PAYMENT_REMINDER -> prefs.isPaymentReminders();
            case CONTRACT_EXPIRY -> prefs.isContractExpiryAlerts();
            case INVITATION_ACCEPTED -> prefs.isNewMemberNotifications();
            default -> true;
        };
    }

    private boolean canSendViaChannel(NotificationChannel channel, SendNotificationRequest request) {
        return switch (channel) {
            case EMAIL -> request.recipientEmail() != null && !request.recipientEmail().isBlank();
            case SMS -> request.recipientPhone() != null && !request.recipientPhone().isBlank();
        };
    }
}
