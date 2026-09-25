package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContactCredit;
import com.buurman.domain.ContactCredit.CreditSource;
import com.buurman.util.MoneyAmount;
import com.buurman.util.SidGenerator;

class ContactCreditRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContactCreditRepository repo;
  private UUID contactA;
  private UUID contactB;

  @BeforeEach
  void setUp() {
    repo = new ContactCreditRepository(dsl, CLOCK);
    contactA = TestDataHelper.insertContact(dsl, TEAM_A_ID, USER_ID);
    contactB = TestDataHelper.insertContact(dsl, TEAM_B_ID, USER_ID);
  }

  private ContactCredit credit(UUID teamId, UUID contactId, String amount, String remaining) {
    return ContactCredit.builder()
        .identifier(Optional.of(SidGenerator.newContactCreditId()))
        .teamId(teamId)
        .contactId(contactId)
        .amount(MoneyAmount.of(new BigDecimal(amount), "EUR"))
        .remainingAmount(new BigDecimal(remaining))
        .source(CreditSource.CREDIT_NOTE)
        .reason(Optional.of("goodwill"))
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  @Test
  @DisplayName("save, read back and update remaining amount and refund")
  void roundTrip() {
    ContactCredit saved = repo.save(credit(TEAM_A_ID, contactA, "120.50", "120.50"));
    assertThat(saved.getId()).isNotNull();

    ContactCredit found =
        repo.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
    assertThat(found.getAmount().value()).isEqualByComparingTo("120.50");
    assertThat(found.getRemainingAmount()).isEqualByComparingTo("120.50");
    assertThat(found.getSource()).isEqualTo(CreditSource.CREDIT_NOTE);
    assertThat(found.getReason()).contains("goodwill");

    found.setRemainingAmount(new BigDecimal("20.50"));
    found.setRefundedAt(Optional.of(Instant.parse("2026-03-05T00:00:00Z")));
    found.setRefundNotes(Optional.of("bank transfer"));
    repo.save(found);

    ContactCredit updated =
        repo.getByIdentifierAndTeamId(found.getIdentifier().orElseThrow(), TEAM_A_ID);
    assertThat(updated.getRemainingAmount()).isEqualByComparingTo("20.50");
    assertThat(updated.getRefundedAt()).contains(Instant.parse("2026-03-05T00:00:00Z"));
    assertThat(updated.getRefundNotes()).contains("bank transfer");
  }

  @Test
  @DisplayName("open credits exclude exhausted ones and other teams")
  void openCreditsAndIsolation() {
    repo.save(credit(TEAM_A_ID, contactA, "50.00", "50.00"));
    repo.save(credit(TEAM_A_ID, contactA, "30.00", "0.00"));
    repo.save(credit(TEAM_B_ID, contactB, "70.00", "70.00"));

    List<ContactCredit> open = repo.findOpenByContactIdAndTeamId(contactA, TEAM_A_ID);
    assertThat(open).hasSize(1);
    assertThat(open.getFirst().getRemainingAmount()).isEqualByComparingTo("50.00");
    assertThat(repo.findByContactIdAndTeamId(contactA, TEAM_A_ID)).hasSize(2);
    assertThat(repo.findByContactIdAndTeamId(contactB, TEAM_A_ID)).isEmpty();
    assertThat(
            repo.findByIdentifierAndTeamId(
                repo.findByContactIdAndTeamId(contactB, TEAM_B_ID)
                    .getFirst()
                    .getIdentifier()
                    .orElseThrow(),
                TEAM_A_ID))
        .isEmpty();
  }
}
