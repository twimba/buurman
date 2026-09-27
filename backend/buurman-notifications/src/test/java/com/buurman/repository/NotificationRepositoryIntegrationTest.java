package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Notification;
import com.buurman.domain.NotificationChannel;
import com.buurman.domain.NotificationStatus;
import com.buurman.domain.NotificationType;
import com.buurman.mapper.NotificationRecordMapper;
import com.buurman.util.SidGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lives in buurman-notifications because that is where NotificationRepository is, and reuses
 * buurman-core's integration base through its test-jar rather than duplicating the Testcontainer
 * bootstrap.
 */
@DisplayName("NotificationRepository Integration")
class NotificationRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private NotificationRepository repo;

  @BeforeEach
  void setUpRepository() {
    ObjectMapper objectMapper = new ObjectMapper();
    repo =
        new NotificationRepository(
            dsl, new NotificationRecordMapper(objectMapper), objectMapper, CLOCK);
  }

  private Notification buildNotification(UUID teamId) {
    Notification notification = new Notification();
    notification.setIdentifier(Optional.of(SidGenerator.newNotificationId()));
    notification.setTeamId(Optional.of(teamId));
    notification.setNotificationType(NotificationType.PAYMENT_REMINDER);
    notification.setChannel(NotificationChannel.EMAIL);
    notification.setStatus(NotificationStatus.SENT);
    notification.setRecipientEmail(Optional.of("jan@example.com"));
    notification.setContentTemplate(Optional.of("payment-reminder"));
    notification.setCreatedBy(Optional.of(USER_ID));
    return notification;
  }

  private UUID aContract() {
    return TestDataHelper.insertContract(
        dsl, TEAM_A_ID, TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID), USER_ID);
  }

  @Nested
  @DisplayName("relatedEntities")
  class RelatedEntities {

    @Test
    @DisplayName("the payment and contract link round-trips")
    void linkRoundTrips() {
      UUID contractId = aContract();
      UUID paymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      Notification notification = buildNotification(TEAM_A_ID);
      notification.setRelatedPaymentId(Optional.of(paymentId));
      notification.setRelatedContractId(Optional.of(contractId));

      Notification saved = repo.save(notification);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID))
          .get()
          .satisfies(
              found -> {
                assertThat(found.getRelatedPaymentId()).isEqualTo(Optional.of(paymentId));
                assertThat(found.getRelatedContractId()).isEqualTo(Optional.of(contractId));
              });
    }

    @Test
    @DisplayName("an unlinked notification round-trips as empty, not null")
    void unlinkedRoundTripsAsEmpty() {
      Notification saved = repo.save(buildNotification(TEAM_A_ID));

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID))
          .get()
          .satisfies(
              found -> {
                assertThat(found.getRelatedPaymentId()).isEmpty();
                assertThat(found.getRelatedContractId()).isEmpty();
              });
    }

    @Test
    @DisplayName("finds only the notifications for that payment")
    void findsOnlyThatPaymentsNotifications() {
      UUID contractId = aContract();
      UUID paymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      UUID otherPaymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      Notification mine = buildNotification(TEAM_A_ID);
      mine.setRelatedPaymentId(Optional.of(paymentId));
      repo.save(mine);
      Notification other = buildNotification(TEAM_A_ID);
      other.setRelatedPaymentId(Optional.of(otherPaymentId));
      repo.save(other);

      assertThat(repo.findByPaymentIdAndTeamId(paymentId, TEAM_A_ID)).hasSize(1);
    }

    @Test
    @DisplayName("another team sees none of them")
    void anotherTeamSeesNone() {
      UUID contractId = aContract();
      UUID paymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      Notification mine = buildNotification(TEAM_A_ID);
      mine.setRelatedPaymentId(Optional.of(paymentId));
      repo.save(mine);

      assertThat(repo.findByPaymentIdAndTeamId(paymentId, TEAM_B_ID)).isEmpty();
    }

    @Test
    @DisplayName("a contract sees the reminders sent for its payments")
    void contractSeesItsPaymentReminders() {
      UUID contractId = aContract();
      UUID paymentId = TestDataHelper.insertPayment(dsl, TEAM_A_ID, contractId, USER_ID);
      Notification reminder = buildNotification(TEAM_A_ID);
      reminder.setRelatedPaymentId(Optional.of(paymentId));
      reminder.setRelatedContractId(Optional.of(contractId));
      repo.save(reminder);

      assertThat(repo.findByContractIdAndTeamId(contractId, TEAM_A_ID)).hasSize(1);
    }
  }

  @Nested
  @DisplayName("providerMessageId lookups")
  class ProviderMessageIdLookups {

    /** Mailgun ids and Twilio SIDs are each unique only within their own provider. */
    private static final String SHARED_ID = "shared-provider-id";

    /** save() does not persist provider_message_id; the sender sets it afterwards. */
    private UUID saveWith(NotificationChannel channel, String providerMessageId) {
      Notification notification = buildNotification(TEAM_A_ID);
      notification.setChannel(channel);
      UUID id = repo.save(notification).getId();
      repo.updateStatus(id, NotificationStatus.SENT, providerMessageId, "queued", null);
      return id;
    }

    @Test
    @DisplayName("an open recorded for one provider does not touch the other's row")
    void openCountIsScopedToTheChannel() {
      UUID emailId = saveWith(NotificationChannel.EMAIL, SHARED_ID);
      UUID smsId = saveWith(NotificationChannel.SMS, SHARED_ID);

      repo.incrementOpenCount(SHARED_ID, NotificationChannel.EMAIL);

      assertThat(repo.findByIdAndTeamId(emailId, TEAM_A_ID))
          .get()
          .extracting("openCount")
          .isEqualTo(1);
      assertThat(repo.findByIdAndTeamId(smsId, TEAM_A_ID))
          .get()
          .extracting("openCount")
          .isEqualTo(0);
    }

    @Test
    @DisplayName("a click recorded for one provider does not touch the other's row")
    void clickCountIsScopedToTheChannel() {
      UUID emailId = saveWith(NotificationChannel.EMAIL, SHARED_ID);
      UUID smsId = saveWith(NotificationChannel.SMS, SHARED_ID);

      repo.incrementClickCount(SHARED_ID, NotificationChannel.EMAIL);

      assertThat(repo.findByIdAndTeamId(emailId, TEAM_A_ID))
          .get()
          .extracting("clickCount")
          .isEqualTo(1);
      assertThat(repo.findByIdAndTeamId(smsId, TEAM_A_ID))
          .get()
          .extracting("clickCount")
          .isEqualTo(0);
    }

    @Test
    @DisplayName("the lookup returns the row belonging to the provider that asked")
    void lookupIsScopedToTheChannel() {
      saveWith(NotificationChannel.EMAIL, SHARED_ID);
      UUID smsId = saveWith(NotificationChannel.SMS, SHARED_ID);

      assertThat(repo.findByProviderMessageId(SHARED_ID, NotificationChannel.SMS))
          .get()
          .extracting(Notification::getId)
          .isEqualTo(smsId);
    }
  }
}
