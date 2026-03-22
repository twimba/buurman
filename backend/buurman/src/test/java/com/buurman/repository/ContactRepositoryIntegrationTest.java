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

import com.buurman.domain.Contact;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.ContactRecordMapperImpl;
import com.buurman.util.PaginationHelper.PaginatedResult;

@DisplayName("ContactRepository Integration")
class ContactRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ContactRepository repo;

  @BeforeEach
  void setUp() {
    repo = new ContactRepository(dsl, new ContactRecordMapperImpl(), CLOCK);
  }

  @Nested
  @DisplayName("save")
  class Save {

    @Test
    @DisplayName("insert creates a new contact with generated ID")
    void insertCreatesContact() {
      Contact contact = TestDataHelper.buildContact(TEAM_A_ID, USER_ID);

      Contact saved = repo.save(contact);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getCreatedAt()).isNotNull();

      Contact found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getFirstName()).isEqualTo("Jan");
      assertThat(found.getLastName()).contains("de Vries");
      assertThat(found.getEmail()).contains("jan@test.io");
    }

    @Test
    @DisplayName("update modifies contact fields")
    void updateModifiesContact() {
      Contact contact = TestDataHelper.buildContact(TEAM_A_ID, USER_ID);
      Contact saved = repo.save(contact);

      saved.setFirstName(Optional.of("Piet"));
      saved.setLastName(Optional.of("Jansen"));
      saved.setDisplayName("Piet Jansen");
      repo.save(saved);

      Contact found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getFirstName()).isEqualTo("Piet");
      assertThat(found.getLastName()).contains("Jansen");
    }

    @Test
    @DisplayName("update respects team_id in WHERE clause")
    void updateRespectsTeamId() {
      Contact contact = TestDataHelper.buildContact(TEAM_A_ID, USER_ID);
      Contact saved = repo.save(contact);

      saved.setTeamId(TEAM_B_ID);
      saved.setFirstName(Optional.of("Hacked"));
      repo.save(saved);

      Contact found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getFirstName()).isEqualTo("Jan");
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    @Test
    @DisplayName("findByIdentifierAndTeamId returns contact")
    void findByIdentifierReturnsContact() {
      Contact saved = repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

      Optional<Contact> found =
          repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByIdentifierAndTeamId with wrong team returns empty")
    void findByIdentifierWrongTeamReturnsEmpty() {
      Contact saved = repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

      assertThat(repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID))
          .isEmpty();
    }

    @Test
    @DisplayName("findAllByTeamId returns only contacts for the given team")
    void findAllByTeamIdReturnsOnlyTeamContacts() {
      repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));
      Contact contactB = TestDataHelper.buildContact(TEAM_B_ID, USER_ID);
      contactB.setFirstName(Optional.of("Bob"));
      contactB.setDisplayName("Bob de Vries");
      contactB.setEmail(Optional.of("bob@test.io"));
      repo.save(contactB);

      List<Contact> teamAContacts = repo.findAllByTeamId(TEAM_A_ID);
      List<Contact> teamBContacts = repo.findAllByTeamId(TEAM_B_ID);

      assertThat(teamAContacts).hasSize(1);
      assertThat(teamAContacts.getFirst().getFirstName()).isEqualTo("Jan");
      assertThat(teamBContacts).hasSize(1);
      assertThat(teamBContacts.getFirst().getFirstName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("findByEmailAndTeamId returns contact matching email")
    void findByEmailAndTeamIdReturnsContact() {
      repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

      Optional<Contact> found = repo.findByEmailAndTeamId("jan@test.io", TEAM_A_ID);

      assertThat(found).isPresent();
      assertThat(found.get().getFirstName()).isEqualTo("Jan");
    }

    @Test
    @DisplayName("findByEmailAndTeamId with wrong team returns empty")
    void findByEmailWrongTeamReturnsEmpty() {
      repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

      assertThat(repo.findByEmailAndTeamId("jan@test.io", TEAM_B_ID)).isEmpty();
    }

    @Test
    @DisplayName("getByIdAndTeamId throws for nonexistent contact")
    void getByIdThrowsForMissing() {
      assertThatThrownBy(() -> repo.getByIdAndTeamId(UUID.randomUUID(), TEAM_A_ID))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("searchByTeamId matches first name case-insensitively")
    void searchMatchesFirstName() {
      repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

      List<Contact> results = repo.searchByTeamId(TEAM_A_ID, "jan");

      assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("searchByTeamId matches email")
    void searchMatchesEmail() {
      repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

      List<Contact> results = repo.searchByTeamId(TEAM_A_ID, "jan@test");

      assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("searchByTeamId does not return results from other teams")
    void searchDoesNotCrossTeams() {
      repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

      List<Contact> results = repo.searchByTeamId(TEAM_B_ID, "jan");

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("pagination")
  class Pagination {

    @Test
    @DisplayName("findAllByTeamIdPaginated returns paginated results")
    void findAllByTeamIdPaginated() {
      repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));
      Contact c2 = TestDataHelper.buildContact(TEAM_A_ID, USER_ID);
      c2.setFirstName(Optional.of("Piet"));
      c2.setDisplayName("Piet de Vries");
      c2.setEmail(Optional.of("piet@test.io"));
      repo.save(c2);

      PaginatedResult<Contact> result =
          repo.findAllByTeamIdPaginated(TEAM_A_ID, null, PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).hasSize(2);
      assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findAllByTeamIdPaginated search filters results")
    void findAllByTeamIdPaginatedWithSearch() {
      repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));
      Contact c2 = TestDataHelper.buildContact(TEAM_A_ID, USER_ID);
      c2.setFirstName(Optional.of("Piet"));
      c2.setDisplayName("Piet de Vries");
      c2.setEmail(Optional.of("piet@test.io"));
      repo.save(c2);

      PaginatedResult<Contact> result =
          repo.findAllByTeamIdPaginated(TEAM_A_ID, "Piet", PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).hasSize(1);
      assertThat(result.items().getFirst().getFirstName()).isEqualTo("Piet");
    }
  }

  @Nested
  @DisplayName("softDelete")
  class SoftDelete {

    @Test
    @DisplayName("soft delete hides contact from find queries")
    void softDeleteHidesContact() {
      Contact saved = repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_A_ID);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isEmpty();
      assertThat(repo.findAllByTeamId(TEAM_A_ID)).isEmpty();
    }

    @Test
    @DisplayName("soft delete with wrong team does nothing")
    void softDeleteWrongTeamDoesNothing() {
      Contact saved = repo.save(TestDataHelper.buildContact(TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_B_ID);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isPresent();
    }
  }
}
