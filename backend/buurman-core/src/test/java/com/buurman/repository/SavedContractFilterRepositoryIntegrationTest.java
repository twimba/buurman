package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.SavedContractFilter;
import com.buurman.mapper.SavedContractFilterRecordMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("SavedContractFilterRepository")
class SavedContractFilterRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private SavedContractFilterRepository repository;
  private UUID userA;
  private UUID userB;

  @BeforeEach
  void setUp() {
    repository =
        new SavedContractFilterRepository(
            dsl, new SavedContractFilterRecordMapperImpl(), new ObjectMapper(), CLOCK);
    userA = USER_ID; // the shared fixture user from AbstractRepositoryIntegrationTest
    userB = UUID.randomUUID();
    TestDataHelper.insertUser(dsl, userB);
  }

  @Test
  @DisplayName(
      "user A's saved filter is invisible to user B on the same team, and B cannot delete it")
  void perUserIsolation() {
    SavedContractFilter saved =
        repository.save(
            SavedContractFilter.builder()
                .teamId(TEAM_A_ID)
                .userId(userA)
                .name("Ending soon")
                .criteria(Map.of("endingWithinDays", 90))
                .build());

    assertThat(repository.findByTeamIdAndUserId(TEAM_A_ID, userA))
        .extracting(SavedContractFilter::getId)
        .containsExactly(saved.getId());
    assertThat(repository.findByTeamIdAndUserId(TEAM_A_ID, userB)).isEmpty();

    assertThatThrownBy(
            () ->
                repository.softDeleteByIdentifierAndTeamIdAndUserId(
                    saved.getIdentifier().orElseThrow(), TEAM_A_ID, userB))
        .isInstanceOf(com.buurman.exception.NotFoundException.class);

    // The filter must still exist and be deletable by its actual owner (userA) — proves B's
    // failed delete attempt above did not silently soft-delete the row.
    assertThat(repository.findByTeamIdAndUserId(TEAM_A_ID, userA)).hasSize(1);
    repository.softDeleteByIdentifierAndTeamIdAndUserId(
        saved.getIdentifier().orElseThrow(), TEAM_A_ID, userA);
    assertThat(repository.findByTeamIdAndUserId(TEAM_A_ID, userA)).isEmpty();
  }
}
