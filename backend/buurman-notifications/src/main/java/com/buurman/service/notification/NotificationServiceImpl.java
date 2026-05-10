package com.buurman.service.notification;

import static com.buurman.domain.NotificationChannel.EMAIL;
import static com.buurman.domain.NotificationChannel.SMS;
import static com.buurman.domain.NotificationStatus.DEMO_BLOCKED;
import static com.buurman.domain.NotificationStatus.PENDING;
import static com.buurman.domain.NotificationType.PHONE_VERIFICATION_CODE;
import static com.buurman.domain.NotificationType.VERIFICATION_CODE;
import static com.buurman.domain.TeamRole.TEAM_ADMIN;
import static com.buurman.domain.TeamRole.TEAM_EDITOR;
import static com.buurman.util.FeatureFlags.BLOCK_EMAIL_NOTIFICATIONS;
import static com.buurman.util.FeatureFlags.BLOCK_SMS_NOTIFICATIONS;
import static com.buurman.util.FeatureFlags.EMAIL_NOTIFICATIONS;
import static com.buurman.util.FeatureFlags.SMS_NOTIFICATIONS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.exception.ExternalServiceException;
import com.buurman.repository.NotificationOutboxRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamPreferencesRepository;
import com.buurman.repository.UserNotificationTypePreferenceRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.FeatureFlagService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationOutboxRepository outboxRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamPreferencesRepository teamPreferencesRepository;
  private final UserPreferencesRepository userPreferencesRepository;
  private final UserRepository userRepository;
  private final UserNotificationTypePreferenceRepository notifTypePrefRepository;
  private final FeatureFlagService featureFlagService;
  private final Map<NotificationChannel, NotificationChannelSender> channelSenders;
  private final ObjectMapper objectMapper;

  public NotificationServiceImpl(
      NotificationRepository notificationRepository,
      NotificationOutboxRepository outboxRepository,
      TeamMemberRepository teamMemberRepository,
      TeamPreferencesRepository teamPreferencesRepository,
      UserPreferencesRepository userPreferencesRepository,
      UserRepository userRepository,
      UserNotificationTypePreferenceRepository notifTypePrefRepository,
      FeatureFlagService featureFlagService,
      List<NotificationChannelSender> senders,
      ObjectMapper objectMapper) {
    this.notificationRepository = notificationRepository;
    this.outboxRepository = outboxRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.teamPreferencesRepository = teamPreferencesRepository;
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

  @Override
  @Transactional
  public void send(SendNotificationRequest request) {
    request = resolveRecipientPhone(request);
    List<NotificationChannel> channels = resolveChannels(request);

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

      boolean deliveryBlocked =
          request.teamId().map(teamId -> isDeliveryBlocked(channel, teamId)).orElse(false);

      Locale recipientLocale = resolveRecipientLocale(request);
      RenderedContent content =
          sender.render(request.templateName(), request.templateVariables(), recipientLocale);

      Notification notification = new Notification();
      notification.setTeamId(request.teamId());
      notification.setNotificationType(request.notificationType());
      notification.setSubject(content.subject());
      notification.setBody(content.body());
      notification.setRecipientEmail(request.recipientEmail());
      notification.setRecipientPhone(request.recipientPhone());
      notification.setRecipientUserId(request.recipientUserId());
      notification.setRecipientContactId(request.recipientContactId());
      notification.setChannel(channel);
      notification.setContentTemplate(Optional.of(request.templateName()));
      notification.setContentVariables(Optional.of(request.templateVariables()));
      notification.setCreatedBy(Optional.of(request.createdBy()));
      notification.setUrgency(request.urgency());

      if (deliveryBlocked) {
        notification.setStatus(DEMO_BLOCKED);
        notificationRepository.save(notification);
        log.info(
            "Demo-blocked {} notification type {} for team {}",
            channel,
            request.notificationType(),
            request.teamId().orElse(null));
        continue;
      }

      notification.setStatus(PENDING);
      notification = notificationRepository.save(notification);

      NotificationOutbox outbox = new NotificationOutbox();
      outbox.setNotificationId(notification.getId());
      outbox.setChannel(channel);
      outbox.setMaxRetries(3);
      outbox.setNotificationType(Optional.of(request.notificationType()));
      outbox.setRecipientUserId(request.recipientUserId());
      outbox.setRecipientEmail(request.recipientEmail());
      outbox.setTeamId(request.teamId());
      outbox.setUrgency(request.urgency());

      NotificationSendRequest sendRequest =
          new NotificationSendRequest(
              Optional.of(notification.getId()),
              request.recipientEmail(),
              request.recipientPhone(),
              content.subject(),
              content.body(),
              Optional.empty(),
              Optional.empty());

      try {
        outbox.setPayload(objectMapper.writeValueAsString(sendRequest));
      } catch (JsonProcessingException e) {
        throw new ExternalServiceException("Failed to serialize send request", e);
      }

      outboxRepository.save(outbox);
    }
  }

  @Override
  @Transactional
  public void sendToTeam(SendNotificationRequest request) {
    if (request.teamId().isEmpty()) {
      return;
    }
    List<TeamMember> members = teamMemberRepository.findByTeamId(request.teamId().get());
    for (TeamMember member : members) {
      if (member.getRole() != TEAM_ADMIN && member.getRole() != TEAM_EDITOR) {
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
                        .recipientUserId(Optional.of(user.getId()))
                        .recipientContactId(request.recipientContactId())
                        .recipientEmail(Optional.of(user.getEmail()))
                        .recipientPhone(user.getPhone())
                        .templateName(request.templateName())
                        .templateVariables(request.templateVariables())
                        .urgency(request.urgency())
                        .createdBy(request.createdBy())
                        .build();
                send(perUser);
              });
    }
  }

  @Override
  @Transactional
  public Notification resend(
      @Nullable UUID teamId, NotificationIdentifier notificationIdentifier, @Nullable UUID userId) {
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

    Optional<String> resentUserLang =
        original
            .getRecipientUserId()
            .flatMap(userPreferencesRepository::findByUserId)
            .map(UserPreferences::getLanguage);
    Locale resentLocale =
        resentUserLang
            .map(Locale::forLanguageTag)
            .orElseGet(
                () ->
                    original
                        .getTeamId()
                        .flatMap(teamPreferencesRepository::findByTeamId)
                        .map(tp -> Locale.forLanguageTag(tp.getDefaultLanguage()))
                        .orElse(Locale.ENGLISH));
    RenderedContent content = sender.render(contentTemplate, contentVariables, resentLocale);

    Notification resent = new Notification();
    resent.setTeamId(original.getTeamId());
    resent.setNotificationType(original.getNotificationType());
    resent.setSubject(content.subject());
    resent.setBody(content.body());
    resent.setRecipientEmail(original.getRecipientEmail());
    resent.setRecipientPhone(original.getRecipientPhone());
    resent.setRecipientUserId(original.getRecipientUserId());
    resent.setRecipientContactId(original.getRecipientContactId());
    resent.setChannel(original.getChannel());
    resent.setContentTemplate(original.getContentTemplate());
    resent.setContentVariables(original.getContentVariables());
    resent.setResentFromId(Optional.of(original.getId()));
    resent.setResendReason(Optional.of("Resent by user"));
    resent.setCreatedBy(Optional.ofNullable(userId));

    NotificationChannel resentChannel = resent.getChannel();
    boolean deliveryBlocked =
        resent.getTeamId().map(tid -> isDeliveryBlocked(resentChannel, tid)).orElse(false);

    if (deliveryBlocked) {
      resent.setStatus(DEMO_BLOCKED);
      resent = notificationRepository.save(resent);
      log.info(
          "Demo-blocked resend of {} notification for team {}",
          resent.getChannel(),
          resent.getTeamId().orElse(null));
      return resent;
    }

    resent.setStatus(NotificationStatus.PENDING);
    resent = notificationRepository.save(resent);

    NotificationOutbox outbox = new NotificationOutbox();
    outbox.setNotificationId(resent.getId());
    outbox.setChannel(resent.getChannel());
    outbox.setMaxRetries(3);
    outbox.setNotificationType(Optional.of(resent.getNotificationType()));
    outbox.setRecipientUserId(resent.getRecipientUserId());
    outbox.setRecipientEmail(resent.getRecipientEmail());
    outbox.setTeamId(resent.getTeamId());
    outbox.setUrgency(resent.getUrgency());

    NotificationSendRequest sendRequest =
        new NotificationSendRequest(
            Optional.of(resent.getId()),
            resent.getRecipientEmail(),
            resent.getRecipientPhone(),
            content.subject(),
            content.body(),
            Optional.empty(),
            Optional.empty());

    try {
      outbox.setPayload(objectMapper.writeValueAsString(sendRequest));
    } catch (JsonProcessingException e) {
      throw new ExternalServiceException("Failed to serialize send request", e);
    }

    outboxRepository.save(outbox);
    return resent;
  }

  private List<NotificationChannel> resolveChannels(SendNotificationRequest request) {
    // No user → EMAIL only (e.g., contact notifications)
    if (request.recipientUserId().isEmpty()) {
      return List.of(EMAIL);
    }

    NotificationType type = request.notificationType();

    // Verification notifications bypass preference checks entirely —
    // they must always be sent via their required channel regardless of user settings
    if (type == VERIFICATION_CODE) {
      return List.of(EMAIL);
    }
    if (type == PHONE_VERIFICATION_CODE) {
      return List.of(SMS);
    }

    boolean smsEnabled = featureFlagService.isEnabled(SMS_NOTIFICATIONS);
    boolean emailFlagEnabled = featureFlagService.isEnabled(EMAIL_NOTIFICATIONS);

    UserPreferences globalPrefs =
        userPreferencesRepository
            .findByUserId(request.recipientUserId().get())
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
            .findByUserIdAndType(request.recipientUserId().get(), type)
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
    if (request.recipientPhone().filter(p -> !p.isBlank()).isPresent()) {
      return request;
    }
    if (request.recipientUserId().isEmpty()) {
      return request;
    }
    return userRepository
        .findById(request.recipientUserId().get())
        .flatMap(User::getPhone)
        .filter(phone -> !phone.isBlank())
        .map(
            phone ->
                SendNotificationRequest.builder()
                    .teamId(request.teamId())
                    .notificationType(request.notificationType())
                    .recipientUserId(request.recipientUserId())
                    .recipientContactId(request.recipientContactId())
                    .recipientEmail(request.recipientEmail())
                    .recipientPhone(Optional.of(phone))
                    .templateName(request.templateName())
                    .templateVariables(request.templateVariables())
                    .urgency(request.urgency())
                    .createdBy(request.createdBy())
                    .build())
        .orElse(request);
  }

  private boolean isDeliveryBlocked(NotificationChannel channel, UUID teamId) {
    return switch (channel) {
      case EMAIL -> featureFlagService.isEnabled(BLOCK_EMAIL_NOTIFICATIONS, teamId);
      case SMS -> featureFlagService.isEnabled(BLOCK_SMS_NOTIFICATIONS, teamId);
    };
  }

  private Locale resolveRecipientLocale(SendNotificationRequest request) {
    // 1. User preference (highest priority)
    Optional<String> userLang =
        request
            .recipientUserId()
            .flatMap(userPreferencesRepository::findByUserId)
            .map(UserPreferences::getLanguage);
    if (userLang.isPresent()) {
      return Locale.forLanguageTag(userLang.get());
    }
    // 2. Team default (fallback)
    return request
        .teamId()
        .flatMap(teamPreferencesRepository::findByTeamId)
        .map(tp -> Locale.forLanguageTag(tp.getDefaultLanguage()))
        .orElse(Locale.ENGLISH);
  }

  private boolean canSendViaChannel(NotificationChannel channel, SendNotificationRequest request) {
    return switch (channel) {
      case EMAIL -> request.recipientEmail().filter(e -> !e.isBlank()).isPresent();
      case SMS -> {
        if (request.recipientPhone().filter(p -> !p.isBlank()).isEmpty()) {
          yield false;
        }
        // Allow phone verification SMS to unverified phones
        if (request.notificationType() == PHONE_VERIFICATION_CODE) {
          yield true;
        }
        // Block other SMS to users with unverified phones
        if (request.recipientUserId().isPresent()) {
          User user = userRepository.findById(request.recipientUserId().get()).orElse(null);
          yield user != null && user.getPhoneVerifiedAt().isPresent();
        }
        yield true; // Non-user SMS (e.g. contact notifications)
      }
    };
  }
}
