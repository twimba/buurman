package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contact;
import com.buurman.mapper.ContactRecordMapperImpl;

@DisplayName("ContactRepository Integration")
class ContactRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContactRepository repo;

  @BeforeEach
  void setUp() {
    repo = new ContactRepository(dsl, new ContactRecordMapperImpl(), CLOCK);
  }

  @Test
  @DisplayName("preferred language round-trips")
  void preferredLanguageRoundTrips() {
    Contact contact = TestDataHelper.buildContact(TEAM_A_ID, USER_ID);
    contact.setPreferredLanguage(Optional.of("pt"));

    Contact saved = repo.save(contact);

    assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID))
        .get()
        .extracting(Contact::getPreferredLanguage)
        .isEqualTo(Optional.of("pt"));
  }

  @Test
  @DisplayName("preferred language defaults to empty")
  void preferredLanguageDefaultsToEmpty() {
    Contact saved = repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

    assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID))
        .get()
        .extracting(Contact::getPreferredLanguage)
        .isEqualTo(Optional.empty());
  }

  @Test
  @DisplayName("an unsupported language code is rejected by the database")
  void unsupportedLanguageIsRejected() {
    Contact contact = TestDataHelper.buildContact(TEAM_A_ID, USER_ID);
    contact.setPreferredLanguage(Optional.of("xx"));

    assertThatThrownBy(() -> repo.save(contact))
        .hasMessageContaining("chk_contact_preferred_language");
  }

  @Test
  @DisplayName("a contact is invisible to another team")
  void contactIsScopedToItsTeam() {
    Contact saved = repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

    assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_B_ID)).isEmpty();
  }
}
