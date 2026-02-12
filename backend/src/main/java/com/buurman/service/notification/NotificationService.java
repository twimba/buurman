package com.buurman.service.notification;

import com.buurman.domain.*;
import com.buurman.repository.NotificationOutboxRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.UserNotificationTypePreferenceRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.repository.UserRepository;
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
    private final TeamMemberRepository teamMemberRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserRepository userRepository;
    private final UserNotificationTypePreferenceRepository notifTypePrefRepository;
    private final Map<NotificationChannel, NotificationChannelSender> channelSenders;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationOutboxRepository outboxRepository,
                               TeamMemberRepository teamMemberRepository,
                               UserPreferencesRepository userPreferencesRepository,
                               UserRepository userRepository,
                               UserNotificationTypePreferenceRepository notifTypePrefRepository,
                               List<NotificationChannelSender> senders,
                               ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.outboxRepository = outboxRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.userRepository = userRepository;
        this.notifTypePrefRepository = notifTypePrefRepository;
        this.objectMapper = objectMapper;

        this.channelSenders = new HashMap<>();
        for (NotificationChannelSender sender : senders) {
            this.channelSenders.put(sender.getChannel(), sender);
        }
    }

    @Transactional
    public List<Notification> send(SendNotificationRequest request) {
        request = resolveRecipientPhone(request);
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
            notification.setBody(content.body());
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
    public List<Notification> sendToTeam(SendNotificationRequest request) {
        List<Notification> allNotifications = new ArrayList<>();
        List<TeamMember> members = teamMemberRepository.findByTeamId(request.teamId());
        for (TeamMember member : members) {
            if (!"TEAM_ADMIN".equals(member.getRole()) && !"TEAM_EDITOR".equals(member.getRole())) {
                continue;
            }
            userRepository.findById(member.getUserId()).ifPresent(user -> {
                SendNotificationRequest perUser = SendNotificationRequest.builder()
                        .teamId(request.teamId())
                        .notificationType(request.notificationType())
                        .recipientUserId(user.getId())
                        .recipientTenantId(request.recipientTenantId())
                        .recipientEmail(user.getEmail())
                        .recipientPhone(user.getPhone())
                        .templateName(request.templateName())
                        .templateVariables(request.templateVariables())
                        .createdBy(request.createdBy())
                        .build();
                allNotifications.addAll(send(perUser));
            });
        }
        return allNotifications;
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
        resent.setBody(content.body());
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
        // No user → EMAIL only (e.g., tenant notifications)
        if (request.recipientUserId() == null) {
            return List.of(NotificationChannel.EMAIL);
        }

        UserPreferences globalPrefs = userPreferencesRepository.findByUserId(request.recipientUserId())
                .orElseGet(UserPreferences::new);

        NotificationType type = request.notificationType();

        // System notification types → all globally-enabled channels
        if (!type.isConfigurable()) {
            List<NotificationChannel> channels = new ArrayList<>();
            if (globalPrefs.isEmailNotifications()) channels.add(NotificationChannel.EMAIL);
            if (globalPrefs.isSmsNotifications()) channels.add(NotificationChannel.SMS);
            return channels.isEmpty() ? List.of(NotificationChannel.EMAIL) : channels;
        }

        // Configurable types → intersect global prefs AND per-type prefs
        UserNotificationTypePreference typePref = notifTypePrefRepository
                .findByUserIdAndType(request.recipientUserId(), type)
                .orElseGet(UserNotificationTypePreference::new);

        List<NotificationChannel> channels = new ArrayList<>();
        if (globalPrefs.isEmailNotifications() && typePref.isEmailEnabled()) {
            channels.add(NotificationChannel.EMAIL);
        }
        if (globalPrefs.isSmsNotifications() && typePref.isSmsEnabled()) {
            channels.add(NotificationChannel.SMS);
        }

        return channels;
    }

    private SendNotificationRequest resolveRecipientPhone(SendNotificationRequest request) {
        if (request.recipientPhone() != null && !request.recipientPhone().isBlank()) {
            return request;
        }
        if (request.recipientUserId() == null) {
            return request;
        }
        return userRepository.findById(request.recipientUserId())
                .map(User::getPhone)
                .filter(phone -> phone != null && !phone.isBlank())
                .map(phone -> SendNotificationRequest.builder()
                        .teamId(request.teamId())
                        .notificationType(request.notificationType())
                        .recipientUserId(request.recipientUserId())
                        .recipientTenantId(request.recipientTenantId())
                        .recipientEmail(request.recipientEmail())
                        .recipientPhone(phone)
                        .templateName(request.templateName())
                        .templateVariables(request.templateVariables())
                        .createdBy(request.createdBy())
                        .build())
                .orElse(request);
    }

    private boolean canSendViaChannel(NotificationChannel channel, SendNotificationRequest request) {
        return switch (channel) {
            case EMAIL -> request.recipientEmail() != null && !request.recipientEmail().isBlank();
            case SMS -> {
                if (request.recipientPhone() == null || request.recipientPhone().isBlank()) {
                    yield false;
                }
                // Allow phone verification SMS to unverified phones
                if (request.notificationType() == NotificationType.PHONE_VERIFICATION_CODE) {
                    yield true;
                }
                // Block other SMS to users with unverified phones
                if (request.recipientUserId() != null) {
                    User user = userRepository.findById(request.recipientUserId()).orElse(null);
                    yield user != null && user.getPhoneVerifiedAt() != null;
                }
                yield true; // Non-user SMS (e.g. tenant notifications)
            }
        };
    }
}
