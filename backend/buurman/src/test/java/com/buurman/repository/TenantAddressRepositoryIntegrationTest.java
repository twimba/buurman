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

import com.buurman.domain.TenantAddress;
import com.buurman.exception.NotFoundException;

@DisplayName("TenantAddressRepository Integration")
class TenantAddressRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private TenantAddressRepository repo;
  private UUID tenantIdA;
  private UUID tenantIdB;

  @BeforeEach
  void setUp() {
    repo = new TenantAddressRepository(dsl, CLOCK);
    tenantIdA = TestDataHelper.insertTenant(dsl, TEAM_A_ID, USER_ID);
    tenantIdB = TestDataHelper.insertTenant(dsl, TEAM_B_ID, USER_ID);
  }

  @Nested
  @DisplayName("save")
  class Save {

    @Test
    @DisplayName("insert creates new address with generated ID and identifier")
    void insertCreatesAddress() {
      TenantAddress address = TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID);

      TenantAddress saved = repo.save(address);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getIdentifier()).isPresent();

      TenantAddress found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getStreet()).isEqualTo("Herengracht 100");
      assertThat(found.getCity()).isEqualTo("Amsterdam");
      assertThat(found.getCountryCode()).isEqualTo("NL");
      assertThat(found.getLatitude()).isPresent();
      assertThat(found.getLongitude()).isPresent();
    }

    @Test
    @DisplayName("update modifies existing address")
    void updateModifiesAddress() {
      TenantAddress address = TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID);
      TenantAddress saved = repo.save(address);

      saved.setStreet("Keizersgracht 200");
      saved.setCity("Rotterdam");
      repo.save(saved);

      TenantAddress found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getStreet()).isEqualTo("Keizersgracht 200");
      assertThat(found.getCity()).isEqualTo("Rotterdam");
    }

    @Test
    @DisplayName("update includes team_id in WHERE clause (multi-tenant safety)")
    void updateRespectsTeamId() {
      TenantAddress address = TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID);
      TenantAddress saved = repo.save(address);

      // Attempt to update with wrong team_id — should silently fail (0 rows updated)
      saved.setTeamId(TEAM_B_ID);
      saved.setStreet("Should Not Change");
      repo.save(saved);

      // Verify original address unchanged
      TenantAddress found = repo.getByIdAndTeamId(saved.getId(), TEAM_A_ID);
      assertThat(found.getStreet()).isEqualTo("Herengracht 100");
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    @Test
    @DisplayName("findByTenantId returns addresses for the correct team")
    void findByTenantIdReturnsForCorrectTeam() {
      repo.save(TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID));

      List<TenantAddress> results = repo.findByTenantId(tenantIdA, TEAM_A_ID);

      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getTeamId()).isEqualTo(TEAM_A_ID);
    }

    @Test
    @DisplayName("findByTenantId with wrong team returns empty (multi-tenant isolation)")
    void findByTenantIdWrongTeamReturnsEmpty() {
      repo.save(TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID));

      List<TenantAddress> results = repo.findByTenantId(tenantIdA, TEAM_B_ID);

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByIdentifierAndTeamId returns address")
    void findByIdentifierReturnsAddress() {
      TenantAddress saved = repo.save(
          TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID));

      Optional<TenantAddress> found =
          repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);

      assertThat(found).isPresent();
      assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByIdentifierAndTeamId with wrong team returns empty")
    void findByIdentifierWrongTeamReturnsEmpty() {
      TenantAddress saved = repo.save(
          TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID));

      Optional<TenantAddress> found =
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
      TenantAddress saved = repo.save(
          TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_A_ID);

      assertThat(repo.findByTenantId(tenantIdA, TEAM_A_ID)).isEmpty();
      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isEmpty();
    }

    @Test
    @DisplayName("soft delete with wrong team does nothing")
    void softDeleteWrongTeamDoesNothing() {
      TenantAddress saved = repo.save(
          TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_B_ID);

      // Address should still be visible
      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isPresent();
    }

    @Test
    @DisplayName("findByTenantId excludes soft-deleted addresses")
    void findByTenantIdExcludesSoftDeleted() {
      TenantAddress addr1 = repo.save(
          TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID));

      // Soft-delete first so unique constraint allows second CURRENT/ACTIVE insert
      repo.softDeleteByIdAndTeamId(addr1.getId(), TEAM_A_ID);

      TenantAddress addr2Src = TestDataHelper.buildTenantAddress(tenantIdA, TEAM_A_ID, USER_ID);
      addr2Src.setStreet("Second Address");
      TenantAddress addr2 = repo.save(addr2Src);

      List<TenantAddress> results = repo.findByTenantId(tenantIdA, TEAM_A_ID);
      assertThat(results).hasSize(1);
      assertThat(results.getFirst().getStreet()).isEqualTo("Second Address");
    }
  }
}
