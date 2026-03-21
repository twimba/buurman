package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.ContactAddress;
import com.buurman.exception.NotFoundException;

@DisplayName("ContactAddressRepository Integration")
class ContactAddressRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContactAddressRepository repo;
  private UUID contactIdA;
  private UUID contactIdB;

  @BeforeEach
  void setUp() {
    repo = new ContactAddressRepository(dsl, CLOCK);
    contactIdA = TestDataHelper.insertContact(dsl, TEAM_A_ID, USER_ID);
    contactIdB = TestDataHelper.insertContact(dsl, TEAM_B_ID, USER_ID);
  }

  @Nested
  @DisplayName("save")
  class Save {

    @Test
    @DisplayName("insert creates new address with generated ID and identifier")
    void insertCreatesAddress() {
      ContactAddress address = TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID);

      ContactAddress saved = repo.save(address);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getIdentifier()).isPresent();

      ContactAddress found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getStreet()).isEqualTo("Herengracht 100");
      assertThat(found.getCity()).isEqualTo("Amsterdam");
      assertThat(found.getCountryCode()).isEqualTo("NL");
      assertThat(found.getLatitude()).isPresent();
      assertThat(found.getLongitude()).isPresent();
    }

    @Test
    @DisplayName("update modifies existing address")
    void updateModifiesAddress() {
      ContactAddress address = TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID);
      ContactAddress saved = repo.save(address);

      saved.setStreet("Keizersgracht 200");
      saved.setCity("Rotterdam");
      repo.save(saved);

      ContactAddress found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getStreet()).isEqualTo("Keizersgracht 200");
      assertThat(found.getCity()).isEqualTo("Rotterdam");
    }

    @Test
    @DisplayName("update includes team_id in WHERE clause (multi-tenant safety)")
    void updateRespectsTeamId() {
      ContactAddress address = TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID);
      ContactAddress saved = repo.save(address);

      saved.setTeamId(TEAM_B_ID);
      saved.setStreet("Should Not Change");
      repo.save(saved);

      ContactAddress found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getStreet()).isEqualTo("Herengracht 100");
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    @Test
    @DisplayName("findByContactId returns addresses for the correct team")
    void findByContactIdReturnsForCorrectTeam() {
      repo.save(TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID));

      List<ContactAddress> results = repo.findByContactId(contactIdA, TEAM_A_ID);

      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getTeamId()).isEqualTo(TEAM_A_ID);
    }

    @Test
    @DisplayName("findByContactId with wrong team returns empty (multi-tenant isolation)")
    void findByContactIdWrongTeamReturnsEmpty() {
      repo.save(TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID));

      List<ContactAddress> results = repo.findByContactId(contactIdA, TEAM_B_ID);

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByIdentifierAndTeamId returns address")
    void findByIdentifierReturnsAddress() {
      ContactAddress saved = repo.save(
          TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID));

      Optional<ContactAddress> found =
          repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByIdentifierAndTeamId with wrong team returns empty")
    void findByIdentifierWrongTeamReturnsEmpty() {
      ContactAddress saved = repo.save(
          TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID));

      Optional<ContactAddress> found =
          repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID);

      assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("getByIdAndTeamId throws NotFoundException for missing address")
    void getByIdThrowsForMissing() {
      assertThatThrownBy(() -> repo.getByIdAndTeamId(UUID.randomUUID(), TEAM_A_ID))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("softDelete")
  class SoftDelete {

    @Test
    @DisplayName("soft delete hides address from find queries")
    void softDeleteHidesAddress() {
      ContactAddress saved = repo.save(
          TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_A_ID);

      assertThat(repo.findByContactId(contactIdA, TEAM_A_ID)).isEmpty();
      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isEmpty();
    }

    @Test
    @DisplayName("soft delete with wrong team does nothing")
    void softDeleteWrongTeamDoesNothing() {
      ContactAddress saved = repo.save(
          TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_B_ID);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isPresent();
    }

    @Test
    @DisplayName("findByContactId excludes soft-deleted addresses")
    void findByContactIdExcludesSoftDeleted() {
      ContactAddress addr1 = repo.save(
          TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(addr1.getId(), TEAM_A_ID);

      ContactAddress addr2Src = TestDataHelper.buildContactAddress(contactIdA, TEAM_A_ID, USER_ID);
      addr2Src.setStreet("Second Address");
      ContactAddress addr2 = repo.save(addr2Src);

      List<ContactAddress> results = repo.findByContactId(contactIdA, TEAM_A_ID);
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getStreet()).isEqualTo("Second Address");
    }
  }
}
