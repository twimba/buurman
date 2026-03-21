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

import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.TeamRecordMapper;
import com.buurman.util.PaginationHelper.PaginatedResult;

@DisplayName("TeamRepository Integration")
class TeamRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private TeamRepository repo;

  @BeforeEach
  void setUp() {
    repo = new TeamRepository(dsl, new TeamRecordMapper(), CLOCK);
  }

  @Nested
  @DisplayName("save")
  class Save {

    @Test
    @DisplayName("insert creates a new team with generated ID")
    void insertCreatesTeam() {
      Team team = TestDataHelper.buildTeam(TEAM_A_ID, USER_ID);

      Team saved = repo.save(team);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getCreatedAt()).isNotNull();

      Team found = repo.getById(saved.getId());
      assertThat(found.getName()).isEqualTo("New Team");
      assertThat(found.isDemo()).isFalse();
    }

    @Test
    @DisplayName("update modifies team name and sets updatedBy")
    void updateModifiesTeam() {
      Team team = TestDataHelper.buildTeam(TEAM_A_ID, USER_ID);
      Team saved = repo.save(team);

      saved.setName("Updated Name");
      saved.setUpdatedBy(USER_ID);
      repo.save(saved);

      Team found = repo.getById(saved.getId());
      assertThat(found.getName()).isEqualTo("Updated Name");
      assertThat(found.getUpdatedBy()).isEqualTo(USER_ID);
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    @Test
    @DisplayName("findById returns existing team")
    void findByIdReturnsTeam() {
      Optional<Team> found = repo.findById(TEAM_A_ID);

      assertThat(found).isPresent();
      assertThat(found.get().getName()).isEqualTo("Team A");
    }

    @Test
    @DisplayName("findById returns empty for nonexistent ID")
    void findByIdReturnsEmptyForMissing() {
      assertThat(repo.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("getById throws NotFoundException for missing team")
    void getByIdThrowsForMissing() {
      assertThatThrownBy(() -> repo.getById(UUID.randomUUID()))
          .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("findByIdentifier returns team by Sid")
    void findByIdentifierReturnsTeam() {
      Team team = repo.getById(TEAM_A_ID);
      Sid identifier = team.getIdentifier().orElseThrow();

      Optional<Team> found = repo.findByIdentifier(identifier);

      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(TEAM_A_ID);
    }

    @Test
    @DisplayName("findByIds returns multiple teams")
    void findByIdsReturnsMultiple() {
      List<Team> teams = repo.findByIds(List.of(TEAM_A_ID, TEAM_B_ID));

      assertThat(teams).hasSize(2);
    }

    @Test
    @DisplayName("findByIds with empty list returns empty")
    void findByIdsWithEmptyListReturnsEmpty() {
      assertThat(repo.findByIds(List.of())).isEmpty();
    }
  }

  @Nested
  @DisplayName("softDelete")
  class SoftDelete {

    @Test
    @DisplayName("soft delete sets deletedAt and hides from findByIds")
    void softDeleteHidesTeam() {
      Team team = TestDataHelper.buildTeam(TEAM_A_ID, USER_ID);
      Team saved = repo.save(team);

      repo.softDeleteById(saved.getId());

      assertThat(repo.findByIds(List.of(saved.getId()))).isEmpty();
      assertThat(repo.findAllActiveTeamIds()).doesNotContain(saved.getId());
    }

    @Test
    @DisplayName("countAll excludes soft-deleted teams")
    void countAllExcludesSoftDeleted() {
      long before = repo.countAll();

      Team team = TestDataHelper.buildTeam(TEAM_A_ID, USER_ID);
      Team saved = repo.save(team);
      assertThat(repo.countAll()).isEqualTo(before + 1);

      repo.softDeleteById(saved.getId());
      assertThat(repo.countAll()).isEqualTo(before);
    }
  }

  @Nested
  @DisplayName("pagination")
  class Pagination {

    @Test
    @DisplayName("findAllPaginated returns teams with pagination metadata")
    void findAllPaginatedReturnsPaginatedResult() {
      PaginatedResult<Team> result = repo.findAllPaginated(PageRequest.of(null, null, null, (SortDirection) null), null);

      assertThat(result.items()).isNotEmpty();
      assertThat(result.totalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("findAllPaginated filters by search term")
    void findAllPaginatedSearchFilters() {
      PaginatedResult<Team> result = repo.findAllPaginated(PageRequest.of(null, null, null, (SortDirection) null), "Team A");

      assertThat(result.items()).hasSize(1);
      assertThat(result.items().getFirst().getName()).isEqualTo("Team A");
    }

    @Test
    @DisplayName("findAllPaginated excludes soft-deleted teams")
    void findAllPaginatedExcludesSoftDeleted() {
      Team team = TestDataHelper.buildTeam(TEAM_A_ID, USER_ID);
      Team saved = repo.save(team);
      repo.softDeleteById(saved.getId());

      PaginatedResult<Team> result = repo.findAllPaginated(PageRequest.of(null, null, null, (SortDirection) null), saved.getName());

      assertThat(result.items()).noneMatch(t -> t.getId().equals(saved.getId()));
    }
  }
}
