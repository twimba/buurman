package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.PropertyOccupancyPeriod;
import com.buurman.domain.PropertyOccupancyPeriod.OccupancyType;
import com.buurman.mapper.PropertyOccupancyPeriodRecordMapper;
import com.buurman.util.SidGenerator;

/**
 * Proves the {@code excl_occupancy_periods_no_overlap} exclusion constraint against the real
 * PostgreSQL constraint (not a mock), for the exact behavior V068 re-scoped: two units of the same
 * property may now overlap, which was impossible when the constraint was keyed by {@code
 * property_id}.
 */
@DisplayName("PropertyOccupancyPeriodRepository — unit-scoped exclusion constraint (V068)")
class PropertyOccupancyPeriodRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private PropertyOccupancyPeriodRepository repository;
  private UUID propertyId;
  private UUID unit1Id;
  private UUID unit2Id;

  @BeforeEach
  void setUpRepository() {
    repository =
        new PropertyOccupancyPeriodRepository(
            dsl, new PropertyOccupancyPeriodRecordMapper(), CLOCK);

    propertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    unit1Id = UUID.randomUUID();
    unit2Id = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unit1Id, propertyId, TEAM_A_ID, "1", "VACANT");
    TestDataHelper.insertUnit(dsl, unit2Id, propertyId, TEAM_A_ID, "2", "VACANT");
  }

  private PropertyOccupancyPeriod period(UUID unitId, LocalDate start, LocalDate end) {
    PropertyOccupancyPeriod period = new PropertyOccupancyPeriod();
    period.setIdentifier(Optional.of(SidGenerator.newOccupancyPeriodId()));
    period.setTeamId(TEAM_A_ID);
    period.setPropertyId(propertyId);
    period.setUnitId(unitId);
    period.setStartDate(start);
    period.setEndDate(Optional.of(end));
    period.setType(OccupancyType.PERSONAL);
    period.setCreatedBy(USER_ID);
    period.setUpdatedBy(USER_ID);
    return period;
  }

  @Test
  @DisplayName("rejects two overlapping self-occupancy periods on the same unit")
  void rejectsOverlapOnSameUnit() {
    repository.save(period(unit1Id, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)));

    PropertyOccupancyPeriod overlapping =
        period(unit1Id, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 7, 31));

    assertThatThrownBy(() -> repository.save(overlapping))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("excl_occupancy_periods_no_overlap");
  }

  @Test
  @DisplayName(
      "allows two overlapping self-occupancy periods on different units of the same property —"
          + " impossible before the unit_id re-scope in V068")
  void allowsOverlapOnDifferentUnits() {
    repository.save(period(unit1Id, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)));

    PropertyOccupancyPeriod onOtherUnit =
        period(unit2Id, LocalDate.of(2026, 4, 15), LocalDate.of(2026, 7, 15));

    PropertyOccupancyPeriod saved = repository.save(onOtherUnit);

    assertThat(saved.getId()).isNotNull();
    assertThat(repository.findByPropertyIdAndTeamId(propertyId, TEAM_A_ID)).hasSize(2);
  }
}
