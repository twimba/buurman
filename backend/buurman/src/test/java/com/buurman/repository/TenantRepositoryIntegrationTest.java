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

import com.buurman.domain.Tenant;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.TenantRecordMapperImpl;
import com.buurman.util.PaginationHelper.PaginatedResult;

@DisplayName("TenantRepository Integration")
class TenantRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private TenantRepository repo;

  @BeforeEach
  void setUp() {
    repo = new TenantRepository(dsl, new TenantRecordMapperImpl(), CLOCK);
  }

  @Nested
  @DisplayName("save")
  class Save {

    @Test
    @DisplayName("insert creates a new tenant with generated ID")
    void insertCreatesTenant() {
      Tenant tenant = TestDataHelper.buildTenant(TEAM_A_ID, USER_ID);

      Tenant saved = repo.save(tenant);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getCreatedAt()).isNotNull();

      Tenant found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getFirstName()).isEqualTo("Jan");
      assertThat(found.getLastName()).contains("de Vries");
      assertThat(found.getEmail()).contains("jan@test.io");
    }

    @Test
    @DisplayName("update modifies tenant fields")
    void updateModifiesTenant() {
      Tenant tenant = TestDataHelper.buildTenant(TEAM_A_ID, USER_ID);
      Tenant saved = repo.save(tenant);

      saved.setFirstName("Piet");
      saved.setLastName(Optional.of("Jansen"));
      repo.save(saved);

      Tenant found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getFirstName()).isEqualTo("Piet");
      assertThat(found.getLastName()).contains("Jansen");
    }

    @Test
    @DisplayName("update respects team_id in WHERE clause")
    void updateRespectsTeamId() {
      Tenant tenant = TestDataHelper.buildTenant(TEAM_A_ID, USER_ID);
      Tenant saved = repo.save(tenant);

      saved.setTeamId(TEAM_B_ID);
      saved.setFirstName("Hacked");
      repo.save(saved);

      Tenant found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getFirstName()).isEqualTo("Jan");
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    @Test
    @DisplayName("findByIdentifierAndTeamId returns tenant")
    void findByIdentifierReturnsTenant() {
      Tenant saved = repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));

      Optional<Tenant> found =
          repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByIdentifierAndTeamId with wrong team returns empty")
    void findByIdentifierWrongTeamReturnsEmpty() {
      Tenant saved = repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));

      assertThat(repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID))
          .isEmpty();
    }

    @Test
    @DisplayName("findAllByTeamId returns only tenants for the given team")
    void findAllByTeamIdReturnsOnlyTeamTenants() {
      repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));
      Tenant tenantB = TestDataHelper.buildTenant(TEAM_B_ID, USER_ID);
      tenantB.setFirstName("Bob");
      tenantB.setEmail(Optional.of("bob@test.io"));
      repo.save(tenantB);

      List<Tenant> teamATenants = repo.findAllByTeamId(TEAM_A_ID);
      List<Tenant> teamBTenants = repo.findAllByTeamId(TEAM_B_ID);

      assertThat(teamATenants).hasSize(1);
      assertThat(teamATenants.getFirst().getFirstName()).isEqualTo("Jan");
      assertThat(teamBTenants).hasSize(1);
      assertThat(teamBTenants.getFirst().getFirstName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("findByEmailAndTeamId returns tenant matching email")
    void findByEmailAndTeamIdReturnsTenant() {
      repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));

      Optional<Tenant> found = repo.findByEmailAndTeamId("jan@test.io", TEAM_A_ID);

      assertThat(found).isPresent();
      assertThat(found.get().getFirstName()).isEqualTo("Jan");
    }

    @Test
    @DisplayName("findByEmailAndTeamId with wrong team returns empty")
    void findByEmailWrongTeamReturnsEmpty() {
      repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));

      assertThat(repo.findByEmailAndTeamId("jan@test.io", TEAM_B_ID)).isEmpty();
    }

    @Test
    @DisplayName("getByIdAndTeamId throws for nonexistent tenant")
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
      repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));

      List<Tenant> results = repo.searchByTeamId(TEAM_A_ID, "jan");

      assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("searchByTeamId matches email")
    void searchMatchesEmail() {
      repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));

      List<Tenant> results = repo.searchByTeamId(TEAM_A_ID, "jan@test");

      assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("searchByTeamId does not return results from other teams")
    void searchDoesNotCrossTeams() {
      repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));

      List<Tenant> results = repo.searchByTeamId(TEAM_B_ID, "jan");

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("pagination")
  class Pagination {

    @Test
    @DisplayName("findAllByTeamIdPaginated returns paginated results")
    void findAllByTeamIdPaginated() {
      repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));
      Tenant t2 = TestDataHelper.buildTenant(TEAM_A_ID, USER_ID);
      t2.setFirstName("Piet");
      t2.setEmail(Optional.of("piet@test.io"));
      repo.save(t2);

      PaginatedResult<Tenant> result =
          repo.findAllByTeamIdPaginated(TEAM_A_ID, null, PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).hasSize(2);
      assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findAllByTeamIdPaginated search filters results")
    void findAllByTeamIdPaginatedWithSearch() {
      repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));
      Tenant t2 = TestDataHelper.buildTenant(TEAM_A_ID, USER_ID);
      t2.setFirstName("Piet");
      t2.setEmail(Optional.of("piet@test.io"));
      repo.save(t2);

      PaginatedResult<Tenant> result =
          repo.findAllByTeamIdPaginated(TEAM_A_ID, "Piet", PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).hasSize(1);
      assertThat(result.items().getFirst().getFirstName()).isEqualTo("Piet");
    }
  }

  @Nested
  @DisplayName("softDelete")
  class SoftDelete {

    @Test
    @DisplayName("soft delete hides tenant from find queries")
    void softDeleteHidesTenant() {
      Tenant saved = repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_A_ID);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isEmpty();
      assertThat(repo.findAllByTeamId(TEAM_A_ID)).isEmpty();
    }

    @Test
    @DisplayName("soft delete with wrong team does nothing")
    void softDeleteWrongTeamDoesNothing() {
      Tenant saved = repo.save(TestDataHelper.buildTenant(TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_B_ID);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isPresent();
    }
  }
}
