package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.jooq.generated.tables.records.TeamsRecord;

@DisplayName("TeamRecordMapper")
class TeamRecordMapperTest {

  private final TeamRecordMapper mapper = new TeamRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("TEA01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<Team> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      TeamsRecord record = createCompleteRecord();

      Optional<Team> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      Team team = result.get();
      assertThat(team.getId()).isEqualTo(ID);
      assertThat(team.getIdentifier()).contains(IDENTIFIER);
      assertThat(team.getName()).isEqualTo("Test Team");
      assertThat(team.isDemo()).isTrue();
      assertThat(team.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(team.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(team.getCreatedBy()).isEqualTo(CREATED_BY);
      assertThat(team.getUpdatedBy()).isEqualTo(UPDATED_BY);
      assertThat(team.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps demo false correctly")
    void mapsDemoFalse() {
      TeamsRecord record = createCompleteRecord();
      record.setDemo(false);

      Optional<Team> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().isDemo()).isFalse();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      TeamsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<Team> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }
  }

  private TeamsRecord createCompleteRecord() {
    TeamsRecord record = new TeamsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setName("Test Team");
    record.setDemo(true);
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
