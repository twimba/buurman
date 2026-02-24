package com.buurman.service.notification;

import static com.buurman.domain.NotificationChannel.EMAIL;
import static com.buurman.domain.NotificationChannel.SMS;
import static com.buurman.domain.NotificationStatus.PENDING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationOutbox;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.NotificationType;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.domain.UserNotificationTypePreference;
import com.buurman.domain.UserPreferences;
import com.buurman.exception.ExternalServiceException;
import com.buurman.repository.NotificationOutboxRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.UserNotificationTypePreferenceRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.FeatureFlagService;
import com.buurman.util.FeatureFlags;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationOutboxRepository outboxRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserPreferencesRepository userPreferencesRepository;
  private final UserRepository userRepository;
  private final UserNotificationTypePreferenceRepository notifTypePrefRepository;
  private final FeatureFlagService featureFlagService;
  private final Map<NotificationChannel, NotificationChannelSender> channelSenders;
  private final ObjectMapper objectMapper;

  public NotificationService(
      NotificationRepository notificationRepository,
      NotificationOutboxRepository outboxRepository,
      TeamMemberRepository teamMemberRepository,
      UserPreferencesRepository userPreferencesRepository,
      UserRepository userRepository,
      UserNotificationTypePreferenceRepository notifTypePrefRepository,
      FeatureFlagService featureFlagService,
      List<NotificationChannelSender> senders,
      ObjectMapper objectMapper) {
    this.notificationRepository = notificationRepository;
    this.outboxRepository = outboxRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.userPreferencesRepository = userPreferencesRepository;
    this.userRepository = userRepository;
    this.notifTypePrefRepository = notifTypePrefRepository;
    this.featureFlagService = featureFlagService;
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
        log.debug(
            "Skipping channel {} for notification type {} - missing recipient info",
            channel,
            request.notificationType());
        continue;
      }

      NotificationChannelSender sender = channelSenders.get(channel);
      if (sender == null) {
        log.warn("No sender registered for channel: {}", channel);
        continue;
      }

      RenderedContent content = sender.render(request.templateName(), request.templateVariables());

      Notification notification = new Notification();
      notification.setTeamId(Optional.ofNullable(request.teamId()));
      notification.setNotificationType(request.notificationType());
      notification.setSubject(Optional.ofNullable(content.subject()));
      notification.setBody(content.body());
      notification.setRecipientEmail(Optional.ofNullable(request.recipientEmail()));
      notification.setRecipientPhone(Optional.ofNullable(request.recipientPhone()));
      notification.setRecipientUserId(Optional.ofNullable(request.recipientUserId()));
      notification.setRecipientTenantId(Optional.ofNullable(request.recipientTenantId()));
      notification.setChannel(channel);
      notification.setContentTemplate(Optional.of(request.templateName()));
      notification.setContentVariables(Optional.of(request.templateVariables()));
      notification.setStatus(PENDING);
      notification.setCreatedBy(Optional.of(request.createdBy()));

      notification = notificationRepository.save(notification);

      NotificationOutbox outbox = new NotificationOutbox();
      outbox.setNotificationId(notification.getId());
      outbox.setChannel(channel);
      outbox.setMaxRetries(3);

      NotificationSendRequest sendRequest =
          new NotificationSendRequest(
              notification.getId(),
              request.recipientEmail(),
              request.recipientPhone(),
              content.subject(),
              content.body(),
              null,
              null);

      try {
        outbox.setPayload(objectMapper.writeValueAsString(sendRequest));
      } catch (JsonProcessingException e) {
        throw new ExternalServiceException("Failed to serialize send request", e);
      }

      outboxRepository.save(outbox);
      notifications.add(notification);
    }

    return notifications;
  }

  @Transactional
  public List<Notification> sendToTeam(SendNotificationRequest request) {
    if (request.teamId() == null) {
      return List.of();
    }
    List<Notification> allNotifications = new ArrayList<>();
    List<TeamMember> members = teamMemberRepository.findByTeamId(request.teamId());
    for (TeamMember member : members) {
      if (!"TEAM_ADMIN".equals(member.getRole()) && !"TEAM_EDITOR".equals(member.getRole())) {
        continue;
      }
      userRepository
          .findById(member.getUserId())
          .ifPresent(
              user -> {
                SendNotificationRequest perUser =
                    SendNotificationRequest.builder()
                        .teamId(request.teamId())
                        .notificationType(request.notificationType())
                        .recipientUserId(user.getId())
                        .recipientTenantId(request.recipientTenantId())
                        .recipientEmail(user.getEmail())
                        .recipientPhone(user.getPhone().orElse(null))
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
  public Notification resend(
      @Nullable UUID teamId, String notificationIdentifier, @Nullable UUID userId) {
    Notification original =
        notificationRepository.getByIdentifierAndTeamId(notificationIdentifier, teamId);

    NotificationChannelSender sender = channelSenders.get(original.getChannel());
    if (sender == null) {
      throw new ExternalServiceException("No sender for channel: " + original.getChannel());
    }

    String contentTemplate = original.getContentTemplate().orElse(null);
    Map<String, Object> contentVariables = original.getContentVariables().orElse(null);
    if (contentTemplate == null || contentVariables == null) {
      throw new ExternalServiceException(
          "Cannot resend notification without content template and variables");
    }

    RenderedContent content = sender.render(contentTemplate, contentVariables);

    Notification resent = new Notification();
    resent.setTeamId(original.getTeamId());
    resent.setNotificationType(original.getNotificationType());
    resent.setSubject(Optional.ofNullable(content.subject()));
    resent.setBody(content.body());
    resent.setRecipientEmail(original.getRecipientEmail());
    resent.setRecipientPhone(original.getRecipientPhone());
    resent.setRecipientUserId(original.getRecipientUserId());
    resent.setRecipientTenantId(original.getRecipientTenantId());
    resent.setChannel(original.getChannel());
    resent.setContentTemplate(original.getContentTemplate());
    resent.setContentVariables(original.getContentVariables());
    resent.setStatus(NotificationStatus.PENDING);
    resent.setResentFromId(Optional.of(original.getId()));
    resent.setResendReason(Optional.of("Resent by user"));
    resent.setCreatedBy(Optional.ofNullable(userId));

    resent = notificationRepository.save(resent);

    NotificationOutbox outbox = new NotificationOutbox();
    outbox.setNotificationId(resent.getId());
    outbox.setChannel(resent.getChannel());
    outbox.setMaxRetries(3);

    NotificationSendRequest sendRequest =
        new NotificationSendRequest(
            resent.getId(),
            resent.getRecipientEmail().orElse(null),
            resent.getRecipientPhone().orElse(null),
            content.subject(),
            content.body(),
            null,
            null);

    try {
      outbox.setPayload(objectMapper.writeValueAsString(sendRequest));
    } catch (JsonProcessingException e) {
      throw new ExternalServiceException("Failed to serialize send request", e);
    }

    outboxRepository.save(outbox);
    return resent;
  }

  private List<NotificationChannel> resolveChannels(SendNotificationRequest request) {
    // No user → EMAIL only (e.g., tenant notifications)
    if (request.recipientUserId() == null) {
      return List.of(EMAIL);
    }

    NotificationType type = request.notificationType();

    // Verification notifications bypass preference checks entirely —
    // they must always be sent via their required channel regardless of user settings
    if (type == NotificationType.VERIFICATION_CODE) {
      return List.of(EMAIL);
    }
    if (type == NotificationType.PHONE_VERIFICATION_CODE) {
      return List.of(SMS);
    }

    boolean smsEnabled = featureFlagService.isEnabled(FeatureFlags.SMS_NOTIFICATIONS);
    boolean emailFlagEnabled = featureFlagService.isEnabled(FeatureFlags.EMAIL_NOTIFICATIONS);

    UserPreferences globalPrefs =
        userPreferencesRepository
            .findByUserId(request.recipientUserId())
            .orElseGet(UserPreferences::new);

    // System notification types (essential) → always eligible for email regardless of flag
    if (!type.isConfigurable()) {
      List<NotificationChannel> channels = new ArrayList<>();
      if (globalPrefs.isEmailNotifications()) {
        channels.add(EMAIL);
      }
      if (smsEnabled && globalPrefs.isSmsNotifications()) {
        channels.add(SMS);
      }
      return channels.isEmpty() ? List.of(EMAIL) : channels;
    }

    // Configurable (business) types → email gated by feature flag
    UserNotificationTypePreference typePref =
        notifTypePrefRepository
            .findByUserIdAndType(request.recipientUserId(), type)
            .orElseGet(UserNotificationTypePreference::new);

    List<NotificationChannel> channels = new ArrayList<>();
    if (emailFlagEnabled && globalPrefs.isEmailNotifications() && typePref.isEmailEnabled()) {
      channels.add(EMAIL);
    }
    if (smsEnabled && globalPrefs.isSmsNotifications() && typePref.isSmsEnabled()) {
      channels.add(SMS);
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
    return userRepository
        .findById(request.recipientUserId())
        .flatMap(User::getPhone)
        .filter(phone -> !phone.isBlank())
        .map(
            phone ->
                SendNotificationRequest.builder()
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
          yield user != null && user.getPhoneVerifiedAt().isPresent();
        }
        yield true; // Non-user SMS (e.g. tenant notifications)
      }
    };
  }
}
