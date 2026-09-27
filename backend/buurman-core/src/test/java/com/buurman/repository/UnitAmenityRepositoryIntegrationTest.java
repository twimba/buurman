package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UnitAmenityRepository")
class UnitAmenityRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private UnitAmenityRepository repository;
  private UUID teamAPropertyId;
  private UUID amenityId;

  @BeforeEach
  void setUpRepository() {
    repository =
        new UnitAmenityRepository(
            dsl, Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC));
    teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    amenityId =
        Objects.requireNonNull(
            dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table("amenities"))
                .limit(1)
                .fetchOne(0, UUID.class),
            "expected at least one seeded amenity");
  }

  @Test
  @DisplayName(
      "softDeleteAllByPropertyIdAndTeamId soft-deletes every amenity link of every unit of the"
          + " property, leaving another team's links untouched (BUUR-106 wave3c Critical 4)")
  void softDeleteAllByPropertyIdAndTeamIdSoftDeletesEveryLinkOfTheProperty() {
    UUID unit1Id = UUID.randomUUID();
    UUID unit2Id = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unit1Id, teamAPropertyId, TEAM_A_ID, "1", "VACANT");
    TestDataHelper.insertUnit(dsl, unit2Id, teamAPropertyId, TEAM_A_ID, "2", "VACANT");
    var link1 = repository.insertLink(unit1Id, amenityId, TEAM_A_ID, USER_ID);
    var link2 = repository.insertLink(unit2Id, amenityId, TEAM_A_ID, USER_ID);

    UUID otherTeamPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    UUID otherTeamUnitId = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, otherTeamUnitId, otherTeamPropertyId, TEAM_B_ID, "1", "VACANT");
    var otherTeamLink = repository.insertLink(otherTeamUnitId, amenityId, TEAM_B_ID, USER_ID);

    int affected =
        repository.softDeleteAllByPropertyIdAndTeamId(teamAPropertyId, TEAM_A_ID, USER_ID);

    assertThat(affected).isEqualTo(2);
    assertThat(deletedAtOf(link1.getId())).isNotNull();
    assertThat(deletedAtOf(link2.getId())).isNotNull();
    assertThat(deletedAtOf(otherTeamLink.getId())).isNull();
  }

  private @Nullable LocalDateTime deletedAtOf(UUID linkId) {
    return dsl.select(DSL.field("deleted_at", LocalDateTime.class))
        .from(DSL.table("unit_amenities"))
        .where(DSL.field("id").eq(linkId))
        .fetchOne(0, LocalDateTime.class);
  }
}
