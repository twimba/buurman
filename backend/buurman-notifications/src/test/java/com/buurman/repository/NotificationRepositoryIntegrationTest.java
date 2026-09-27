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
  }
}
