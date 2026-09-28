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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
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
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.TeamMember;
import com.buurman.domain.TeamRole;
import com.buurman.domain.User;
import com.buurman.domain.UserNotificationTypePreference;
import com.buurman.domain.UserPreferences;
import com.buurman.domain.identifier.NotificationIdentifier;
import com.buurman.repository.NotificationOutboxRepository;
import com.buurman.repository.NotificationRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.UserNotificationTypePreferenceRepository;
import com.buurman.repository.UserPreferencesRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.FeatureFlagService;
import com.buurman.util.SidGenerator;
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
  @Mock private RecipientLocaleResolver recipientLocaleResolver;
  @Mock private NotificationChannelSender emailSender;

  private NotificationServiceImpl service;

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    lenient().when(emailSender.getChannel()).thenReturn(EMAIL);
    // The resolver is exercised by RecipientLocaleResolverTest; here it just needs to yield a
    // locale, since an unstubbed mock returns null and render() would not match its stub.
    lenient()
        .when(recipientLocaleResolver.resolve(any(), any(), any(), any()))
        .thenReturn(Locale.ENGLISH);
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
            mapper,
            recipientLocaleResolver);
  }

  private SendNotificationRequest.SendNotificationRequestBuilder baseRequest() {
    return SendNotificationRequest.builder()
        .teamId(Optional.of(TEAM_ID))
        .notificationType(PAYMENT_REMINDER)
        .recipientUserId(Optional.of(USER_ID))
        .recipientEmail(Optional.of("test@example.com"))
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
    when(emailSender.render(anyString(), anyMap(), any(java.util.Locale.class)))
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
              .teamId(Optional.of(TEAM_ID))
              .notificationType(PAYMENT_REMINDER)
              .recipientEmail(Optional.of("tenant@example.com"))
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

      ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
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

  @Nested
  @DisplayName("sendToTeam")
  class SendToTeam {

    @Test
    @DisplayName("sends notification to admin and editor members")
    void sendsToAdminAndEditorMembers() {
      UUID adminUserId = UUID.randomUUID();
      UUID editorUserId = UUID.randomUUID();
      UUID viewerUserId = UUID.randomUUID();

      TeamMember admin =
          TeamMember.builder()
              .id(UUID.randomUUID())
              .teamId(TEAM_ID)
              .userId(adminUserId)
              .role(TeamRole.TEAM_ADMIN)
              .build();
      TeamMember editor =
          TeamMember.builder()
              .id(UUID.randomUUID())
              .teamId(TEAM_ID)
              .userId(editorUserId)
              .role(TeamRole.TEAM_EDITOR)
              .build();
      TeamMember viewer =
          TeamMember.builder()
              .id(UUID.randomUUID())
              .teamId(TEAM_ID)
              .userId(viewerUserId)
              .role(TeamRole.TEAM_VIEWER)
              .build();

      when(teamMemberRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(admin, editor, viewer));

      User adminUser =
          User.builder()
              .id(adminUserId)
              .email("admin@example.com")
              .firstName("Admin")
              .lastName("User")
              .build();
      User editorUser =
          User.builder()
              .id(editorUserId)
              .email("editor@example.com")
              .firstName("Editor")
              .lastName("User")
              .build();

      when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));
      when(userRepository.findById(editorUserId)).thenReturn(Optional.of(editorUser));

      // Stub for VERIFICATION_CODE channel resolution (bypasses prefs)
      stubEmailRender();
      stubNotificationSave();

      SendNotificationRequest request =
          SendNotificationRequest.builder()
              .teamId(Optional.of(TEAM_ID))
              .notificationType(VERIFICATION_CODE)
              .templateName("verification-code")
              .templateVariables(Map.of())
              .createdBy(CREATED_BY)
              .build();

      service.sendToTeam(request);

      // Viewer should be skipped, admin and editor should each get a send() call
      verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("carries the context language and attachments onto every member's notification")
    void sendToTeamCarriesContextAndAttachments() throws Exception {
      UUID adminUserId = UUID.randomUUID();
      when(teamMemberRepository.findByTeamId(TEAM_ID))
          .thenReturn(
              List.of(
                  TeamMember.builder()
                      .userId(adminUserId)
                      .teamId(TEAM_ID)
                      .role(TeamRole.TEAM_ADMIN)
                      .build()));
      when(userRepository.findById(adminUserId))
          .thenReturn(
              Optional.of(
                  User.builder()
                      .id(adminUserId)
                      .email("admin@example.com")
                      .firstName("Admin")
                      .lastName("User")
                      .build()));
      stubEmailRender();
      stubNotificationSave();

      service.sendToTeam(
          SendNotificationRequest.builder()
              .teamId(Optional.of(TEAM_ID))
              .notificationType(VERIFICATION_CODE)
              .templateName("verification-code")
              .templateVariables(Map.of())
              .createdBy(CREATED_BY)
              .contextLanguageTag(Optional.of("nl"))
              .attachments(List.of(new EmailAttachment("notice.pdf", "application/pdf", "k/n.pdf")))
              .build());

      // The per-member rebuild discards silently anything it forgets to copy. No caller sets
      // these on a sendToTeam path today, so this pins the trap rather than a live bug: the next
      // person to attach a PDF to a team notification would watch it vanish.
      ArgumentCaptor<NotificationOutbox> outboxCaptor =
          ArgumentCaptor.forClass(NotificationOutbox.class);
      verify(outboxRepository, atLeastOnce()).save(outboxCaptor.capture());
      assertThat(outboxCaptor.getValue().getPayload()).contains("notice.pdf");

      // The resolver is mocked here, so the rendered locale cannot show whether the context tag
      // survived; what it receives can. This is the argument sendToTeam was dropping.
      @SuppressWarnings("unchecked")
      ArgumentCaptor<Optional<String>> contextCaptor = ArgumentCaptor.forClass(Optional.class);
      verify(recipientLocaleResolver, atLeastOnce())
          .resolve(any(), any(), any(), contextCaptor.capture());
      assertThat(contextCaptor.getAllValues()).contains(Optional.of("nl"));
    }

    @Test
    @DisplayName("carries the entity link onto every member's notification")
    void sendToTeamCarriesTheEntityLink() {
      UUID adminUserId = UUID.randomUUID();
      TeamMember admin =
          TeamMember.builder()
              .userId(adminUserId)
              .teamId(TEAM_ID)
              .role(TeamRole.TEAM_ADMIN)
              .build();
      when(teamMemberRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(admin));
      when(userRepository.findById(adminUserId))
          .thenReturn(
              Optional.of(
                  User.builder()
                      .id(adminUserId)
                      .email("admin@example.com")
                      .firstName("Admin")
                      .lastName("User")
                      .build()));
      stubEmailRender();
      stubNotificationSave();
      UUID paymentId = UUID.randomUUID();
      UUID contractId = UUID.randomUUID();

      service.sendToTeam(
          SendNotificationRequest.builder()
              .teamId(Optional.of(TEAM_ID))
              .notificationType(VERIFICATION_CODE)
              .templateName("verification-code")
              .templateVariables(Map.of())
              .createdBy(CREATED_BY)
              .relatedPaymentId(Optional.of(paymentId))
              .relatedContractId(Optional.of(contractId))
              .build());

      // sendToTeam rebuilds a per-member request; anything it forgets to copy is discarded
      // silently. Ten of the thirteen call sites that set the link reach send() this way.
      ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
      verify(notificationRepository, atLeastOnce()).save(captor.capture());
      assertThat(captor.getAllValues())
          .allSatisfy(
              saved -> {
                assertThat(saved.getRelatedPaymentId()).isEqualTo(Optional.of(paymentId));
                assertThat(saved.getRelatedContractId()).isEqualTo(Optional.of(contractId));
              });
    }

    @Test
    @DisplayName("returns immediately when teamId is empty")
    void returnsImmediatelyWhenTeamIdEmpty() {
      SendNotificationRequest request =
          SendNotificationRequest.builder()
              .notificationType(PAYMENT_REMINDER)
              .templateName("payment-reminder")
              .templateVariables(Map.of())
              .createdBy(CREATED_BY)
              .build();

      service.sendToTeam(request);

      verify(teamMemberRepository, never()).findByTeamId(any());
      verify(notificationRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("canSendViaChannel")
  class CanSendViaChannel {

    @Test
    @DisplayName("skips channel when email is blank")
    void skipsChannelWhenEmailBlank() {
      stubConfigurableChannelResolution(true, false, true, true);

      // Recipient email is blank
      SendNotificationRequest request = baseRequest().recipientEmail(Optional.of("  ")).build();

      service.send(request);

      // Should not save to outbox because canSendViaChannel returns false for blank email
      verify(outboxRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("No sender for channel")
  class NoSenderForChannel {

    @Test
    @DisplayName("logs warning when no sender registered for resolved channel")
    void logWarningWhenNoSenderForChannel() {
      // Create a service with no channel senders at all
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new Jdk8Module());
      NotificationServiceImpl emptyService =
          new NotificationServiceImpl(
              notificationRepository,
              outboxRepository,
              teamMemberRepository,
              userPreferencesRepository,
              userRepository,
              notifTypePrefRepository,
              featureFlagService,
              List.of(), // No senders
              mapper,
              recipientLocaleResolver);

      // This request should resolve to EMAIL channel (no user = default EMAIL)
      SendNotificationRequest request =
          SendNotificationRequest.builder()
              .teamId(Optional.of(TEAM_ID))
              .notificationType(PAYMENT_REMINDER)
              .recipientEmail(Optional.of("test@example.com"))
              .templateName("payment-reminder")
              .templateVariables(Map.of())
              .createdBy(CREATED_BY)
              .build();

      // Should not throw, just log warning and skip
      emptyService.send(request);

      // No notification saved, no outbox saved (sender was null)
      verify(notificationRepository, never()).save(any());
      verify(outboxRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("entity link")
  class EntityLink {

    @Test
    @DisplayName("a send persists the payment and contract it is about")
    void sendPersistsTheLink() {
      UUID paymentId = UUID.randomUUID();
      UUID contractId = UUID.randomUUID();
      stubConfigurableChannelResolution(true, false, true, true);
      stubEmailRender();
      stubNotificationSave();

      service.send(
          baseRequest()
              .relatedPaymentId(Optional.of(paymentId))
              .relatedContractId(Optional.of(contractId))
              .build());

      ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
      verify(notificationRepository, atLeastOnce()).save(captor.capture());
      assertThat(captor.getAllValues())
          .anySatisfy(
              saved -> {
                assertThat(saved.getRelatedPaymentId()).isEqualTo(Optional.of(paymentId));
                assertThat(saved.getRelatedContractId()).isEqualTo(Optional.of(contractId));
              });
    }

    @Test
    @DisplayName("a resend stays on the same timeline as the original")
    void resendCopiesTheLink() {
      UUID paymentId = UUID.randomUUID();
      UUID contractId = UUID.randomUUID();
      NotificationIdentifier identifier = SidGenerator.newNotificationId();
      Notification original = new Notification();
      original.setId(UUID.randomUUID());
      original.setIdentifier(Optional.of(identifier));
      original.setTeamId(Optional.of(TEAM_ID));
      original.setNotificationType(PAYMENT_REMINDER);
      original.setChannel(EMAIL);
      original.setStatus(NotificationStatus.SENT);
      original.setRecipientEmail(Optional.of("jan@example.com"));
      original.setContentTemplate(Optional.of("payment-reminder"));
      original.setContentVariables(Optional.of(Map.of()));
      original.setRelatedPaymentId(Optional.of(paymentId));
      original.setRelatedContractId(Optional.of(contractId));
      stubEmailRender();
      stubNotificationSave();
      when(notificationRepository.getByIdentifierAndTeamId(identifier, TEAM_ID))
          .thenReturn(original);

      service.resend(TEAM_ID, identifier, USER_ID);

      ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
      verify(notificationRepository, atLeastOnce()).save(captor.capture());
      // Without this the landlord resends from the timeline and the new message is absent
      // from the very timeline they are watching.
      assertThat(captor.getAllValues())
          .anySatisfy(
              saved -> {
                assertThat(saved.getRelatedPaymentId()).isEqualTo(Optional.of(paymentId));
                assertThat(saved.getRelatedContractId()).isEqualTo(Optional.of(contractId));
              });
    }
  }
}
