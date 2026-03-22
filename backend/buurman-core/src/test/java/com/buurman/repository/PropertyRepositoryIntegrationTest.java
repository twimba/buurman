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

import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.Property.PropertyStatus;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PropertyRecordMapperImpl;
import com.buurman.util.PaginationHelper.PaginatedResult;

@DisplayName("PropertyRepository Integration")
class PropertyRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private PropertyRepository repo;

  @BeforeEach
  void setUp() {
    repo =
        new PropertyRepository(
            dsl, TestDataHelper.wireMapper(new PropertyRecordMapperImpl()), CLOCK);
  }

  @Nested
  @DisplayName("save")
  class Save {

    @Test
    @DisplayName("insert creates a new property with generated ID")
    void insertCreatesProperty() {
      Property property = TestDataHelper.buildProperty(TEAM_A_ID, USER_ID);

      Property saved = repo.save(property);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getCreatedAt()).isNotNull();

      Property found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getStreet()).isEqualTo("Main Street 1");
      assertThat(found.getCity()).isEqualTo("Amsterdam");
      assertThat(found.getPropertyCategory()).isEqualTo(PropertyCategory.RESIDENTIAL);
    }

    @Test
    @DisplayName("update modifies property fields but NOT propertyCategory (immutable)")
    void updateDoesNotChangeCategory() {
      Property property = TestDataHelper.buildProperty(TEAM_A_ID, USER_ID);
      Property saved = repo.save(property);

      saved.setStreet("Updated Street");
      saved.setPropertyCategory(PropertyCategory.COMMERCIAL);
      repo.save(saved);

      Property found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getStreet()).isEqualTo("Updated Street");
      // Category should remain RESIDENTIAL (immutable on update)
      assertThat(found.getPropertyCategory()).isEqualTo(PropertyCategory.RESIDENTIAL);
    }

    @Test
    @DisplayName("update respects team_id in WHERE clause")
    void updateRespectsTeamId() {
      Property property = TestDataHelper.buildProperty(TEAM_A_ID, USER_ID);
      Property saved = repo.save(property);

      saved.setTeamId(TEAM_B_ID);
      saved.setStreet("Hacked Street");
      repo.save(saved);

      Property found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getStreet()).isEqualTo("Main Street 1");
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    @Test
    @DisplayName("findByIdentifierAndTeamId returns property")
    void findByIdentifierReturnsProperty() {
      Property saved = repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));

      Optional<Property> found =
          repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByIdentifierAndTeamId with wrong team returns empty")
    void findByIdentifierWrongTeamReturnsEmpty() {
      Property saved = repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));

      assertThat(repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID))
          .isEmpty();
    }

    @Test
    @DisplayName("findAllByTeamId returns only properties for the given team")
    void findAllByTeamIdIsolatesTeams() {
      repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));
      Property propB = TestDataHelper.buildProperty(TEAM_B_ID, USER_ID);
      propB.setStreet("Other Street");
      repo.save(propB);

      List<Property> teamA = repo.findAllByTeamId(TEAM_A_ID);
      List<Property> teamB = repo.findAllByTeamId(TEAM_B_ID);

      assertThat(teamA).hasSize(1);
      assertThat(teamA.getFirst().getStreet()).isEqualTo("Main Street 1");
      assertThat(teamB).hasSize(1);
      assertThat(teamB.getFirst().getStreet()).isEqualTo("Other Street");
    }

    @Test
    @DisplayName("findByTeamIdAndStatus filters by status")
    void findByTeamIdAndStatusFilters() {
      repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));
      Property vacant = TestDataHelper.buildProperty(TEAM_A_ID, USER_ID);
      vacant.setStatus(PropertyStatus.VACANT);
      repo.save(vacant);

      List<Property> occupied = repo.findByTeamIdAndStatus(TEAM_A_ID, PropertyStatus.OCCUPIED);
      List<Property> vacantList = repo.findByTeamIdAndStatus(TEAM_A_ID, PropertyStatus.VACANT);

      assertThat(occupied).hasSize(1);
      assertThat(vacantList).hasSize(1);
    }

    @Test
    @DisplayName("getByIdAndTeamId throws for missing property")
    void getByIdThrowsForMissing() {
      assertThatThrownBy(() -> repo.getByIdAndTeamId(UUID.randomUUID(), TEAM_A_ID))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("findByIdsAndTeamId returns matching properties")
    void findByIdsReturnsMatching() {
      Property p1 = repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));
      Property p2 = TestDataHelper.buildProperty(TEAM_A_ID, USER_ID);
      p2.setStreet("Second Street");
      p2 = repo.save(p2);

      List<Property> found = repo.findByIdsAndTeamId(List.of(p1.getId(), p2.getId()), TEAM_A_ID);

      assertThat(found).hasSize(2);
    }

    @Test
    @DisplayName("findByIdsAndTeamId with wrong team returns empty")
    void findByIdsWrongTeamReturnsEmpty() {
      Property p = repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));

      assertThat(repo.findByIdsAndTeamId(List.of(p.getId()), TEAM_B_ID)).isEmpty();
    }
  }

  @Nested
  @DisplayName("pagination")
  class Pagination {

    @Test
    @DisplayName("findAllByTeamIdPaginated with status filter")
    void paginatedWithStatusFilter() {
      repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));
      Property vacant = TestDataHelper.buildProperty(TEAM_A_ID, USER_ID);
      vacant.setStatus(PropertyStatus.VACANT);
      repo.save(vacant);

      PaginatedResult<Property> result =
          repo.findAllByTeamIdPaginated(
              TEAM_A_ID,
              "OCCUPIED",
              null,
              null,
              PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).hasSize(1);
      assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAllByTeamIdPaginated with category filter")
    void paginatedWithCategoryFilter() {
      repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));

      PaginatedResult<Property> result =
          repo.findAllByTeamIdPaginated(
              TEAM_A_ID,
              null,
              "RESIDENTIAL",
              null,
              PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("findAllByTeamIdPaginated with query searches street")
    void paginatedWithQuerySearchesStreet() {
      repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));

      PaginatedResult<Property> result =
          repo.findAllByTeamIdPaginated(
              TEAM_A_ID,
              null,
              null,
              "main",
              PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("findAllByTeamIdPaginated does not return other team's properties")
    void paginatedIsolatesTeams() {
      repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));

      PaginatedResult<Property> result =
          repo.findAllByTeamIdPaginated(
              TEAM_B_ID, null, null, null, PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).isEmpty();
    }
  }

  @Nested
  @DisplayName("softDelete")
  class SoftDelete {

    @Test
    @DisplayName("soft delete hides property from find queries")
    void softDeleteHidesProperty() {
      Property saved = repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_A_ID);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isEmpty();
      assertThat(repo.findAllByTeamId(TEAM_A_ID)).isEmpty();
    }

    @Test
    @DisplayName("soft delete with wrong team does nothing")
    void softDeleteWrongTeamDoesNothing() {
      Property saved = repo.save(TestDataHelper.buildProperty(TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_B_ID);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isPresent();
    }
  }
}
