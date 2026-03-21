package com.buurman.service.notification;

import static com.buurman.domain.NotificationChannel.EMAIL;
import static com.buurman.domain.NotificationStatus.DEMO_BLOCKED;
import static com.buurman.domain.NotificationStatus.PENDING;
import static com.buurman.domain.NotificationType.PAYMENT_REMINDER;
import static com.buurman.domain.NotificationType.VERIFICATION_CODE;
import static com.buurman.domain.NotificationType.WELCOME;
import static com.buurman.util.FeatureFlags.BLOCK_EMAIL_NOTIFICATIONS;
import static com.buurman.util.FeatureFlags.EMAIL_NOTIFICATIONS;
import static com.buurman.util.FeatureFlags.SMS_NOTIFICATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationOutbox;
import com.buurman.domain.UserNotificationTypePreference;
import com.buurman.domain.UserPreferences;
import com.buurman.repository.NotificationOutboxRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.UserNotificationTypePreferenceRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.FeatureFlagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl")
class NotificationServiceImplTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private NotificationOutboxRepository outboxRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private UserPreferencesRepository userPreferencesRepository;
  @Mock private UserRepository userRepository;
  @Mock private UserNotificationTypePreferenceRepository notifTypePrefRepository;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private NotificationChannelSender emailSender;

  private NotificationServiceImpl service;

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    lenient().when(emailSender.getChannel()).thenReturn(EMAIL);
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    service =
        new NotificationServiceImpl(
            notificationRepository,
            outboxRepository,
            teamMemberRepository,
            userPreferencesRepository,
            userRepository,
            notifTypePrefRepository,
            featureFlagService,
            List.of(emailSender),
            mapper);
  }

  private SendNotificationRequest.Builder baseRequest() {
    return SendNotificationRequest.builder()
        .teamId(TEAM_ID)
        .notificationType(PAYMENT_REMINDER)
        .recipientUserId(USER_ID)
        .recipientEmail("test@example.com")
        .templateName("payment-reminder")
        .templateVariables(Map.of("key", "value"))
        .createdBy(CREATED_BY);
  }

  private void stubNotificationSave() {
    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(
            inv -> {
              Notification n = inv.getArgument(0);
              n.setId(UUID.randomUUID());
              return n;
            });
  }

  private void stubEmailRender() {
    when(emailSender.render(anyString(), anyMap()))
        .thenReturn(new RenderedContent(Optional.of("Subject"), "Body", EMAIL));
  }

  private void stubConfigurableChannelResolution(
      boolean emailFlag, boolean smsFlag, boolean globalEmail, boolean typePrefEmail) {
    lenient().when(featureFlagService.isEnabled(EMAIL_NOTIFICATIONS)).thenReturn(emailFlag);
    lenient().when(featureFlagService.isEnabled(SMS_NOTIFICATIONS)).thenReturn(smsFlag);
    lenient()
        .when(userPreferencesRepository.findByUserId(USER_ID))
        .thenReturn(
            Optional.of(
                UserPreferences.builder()
                    .userId(USER_ID)
                    .emailNotifications(globalEmail)
                    .smsNotifications(false)
                    .build()));
    lenient()
        .when(notifTypePrefRepository.findByUserIdAndType(USER_ID, PAYMENT_REMINDER))
        .thenReturn(
            Optional.of(
                UserNotificationTypePreference.builder()
                    .userId(USER_ID)
                    .emailEnabled(typePrefEmail)
                    .smsEnabled(false)
                    .build()));
  }

  @Nested
  @DisplayName("Channel Resolution")
  class ChannelResolution {

    @Test
    @DisplayName("selects EMAIL when globally enabled AND per-type enabled")
    void emailSelectedWhenBothPrefsEnabled() {
      stubConfigurableChannelResolution(true, false, true, true);
      stubEmailRender();
      stubNotificationSave();

      service.send(baseRequest().build());

      verify(outboxRepository).save(any(NotificationOutbox.class));
    }

    @Test
    @DisplayName("returns empty channels when email globally disabled for configurable type")
    void noChannelWhenEmailGloballyDisabled() {
      stubConfigurableChannelResolution(false, false, true, true);

      service.send(baseRequest().build());

      verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("returns empty channels when user disables email globally")
    void noChannelWhenUserEmailDisabled() {
      stubConfigurableChannelResolution(true, false, false, true);

      service.send(baseRequest().build());

      verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("VERIFICATION_CODE always uses EMAIL regardless of preferences")
    void verificationCodeAlwaysUsesEmail() {
      stubEmailRender();
      stubNotificationSave();

      service.send(
          baseRequest()
              .notificationType(VERIFICATION_CODE)
              .templateName("verification-code")
              .build());

      verify(outboxRepository).save(any(NotificationOutbox.class));
    }

    @Test
    @DisplayName("no recipient user ID defaults to EMAIL only")
    void noUserDefaultsToEmail() {
      stubEmailRender();
      stubNotificationSave();

      SendNotificationRequest request =
          SendNotificationRequest.builder()
              .teamId(TEAM_ID)
              .notificationType(PAYMENT_REMINDER)
              .recipientEmail("tenant@example.com")
              .templateName("payment-reminder")
              .templateVariables(Map.of())
              .createdBy(CREATED_BY)
              .build();

      service.send(request);

      verify(outboxRepository).save(any(NotificationOutbox.class));
    }

    @Test
    @DisplayName("system notification types fall back to EMAIL when channels empty")
    void systemTypesFallbackToEmail() {
      lenient()
          .when(userPreferencesRepository.findByUserId(USER_ID))
          .thenReturn(
              Optional.of(
                  UserPreferences.builder()
                      .userId(USER_ID)
                      .emailNotifications(false)
                      .smsNotifications(false)
                      .build()));
      stubEmailRender();
      stubNotificationSave();

      service.send(baseRequest().notificationType(WELCOME).templateName("welcome").build());

      verify(outboxRepository).save(any(NotificationOutbox.class));
    }
  }

  @Nested
  @DisplayName("Delivery Blocking")
  class DeliveryBlocking {

    @Test
    @DisplayName("demo-blocks notification when BLOCK_EMAIL_NOTIFICATIONS flag enabled for team")
    void demoBlockedWhenFlagEnabled() {
      stubConfigurableChannelResolution(true, false, true, true);
      when(featureFlagService.isEnabled(BLOCK_EMAIL_NOTIFICATIONS, TEAM_ID)).thenReturn(true);
      stubEmailRender();

      service.send(baseRequest().build());

      ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
      verify(notificationRepository).save(captor.capture());
      assertThat(captor.getValue().getStatus()).isEqualTo(DEMO_BLOCKED);
      verify(outboxRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Outbox Record Creation")
  class OutboxCreation {

    @Test
    @DisplayName("creates outbox record with correct channel and max retries")
    void createsOutboxRecord() {
      stubConfigurableChannelResolution(true, false, true, true);
      stubEmailRender();
      stubNotificationSave();

      service.send(baseRequest().build());

      ArgumentCaptor<NotificationOutbox> captor =
          ArgumentCaptor.forClass(NotificationOutbox.class);
      verify(outboxRepository).save(captor.capture());
      NotificationOutbox outbox = captor.getValue();
      assertThat(outbox.getChannel()).isEqualTo(EMAIL);
      assertThat(outbox.getMaxRetries()).isEqualTo(3);
      assertThat(outbox.getPayload()).isNotBlank();
    }

    @Test
    @DisplayName("notification is saved with PENDING status before outbox creation")
    void notificationSavedAsPending() {
      stubConfigurableChannelResolution(true, false, true, true);
      stubEmailRender();
      stubNotificationSave();

      service.send(baseRequest().build());

      ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
      verify(notificationRepository).save(captor.capture());
      assertThat(captor.getValue().getStatus()).isEqualTo(PENDING);
    }
  }
}
