package com.buurman.service.demo;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.repository.UnitRepository;
import com.buurman.util.SidGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates one implicit unit per demo property (BUUR-106). {@code contracts.unit_id}, {@code
 * property_occupancy_periods.unit_id} and {@code wws_calculations.unit_id} are all NOT NULL as of
 * V068, so every property generated for demo data needs a unit before contracts are generated.
 * Mirrors V068's own backfill: a single "unit_number = 1" implicit unit. {@link
 * DemoContractGenerator} later flips it to OCCUPIED for properties with an active contract.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DemoUnitGenerator {

  private final UnitRepository unitRepository;
  private final Clock clock;

  public void generate(DemoDataContext ctx) {
    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);
      List<UUID> propertyIds = ctx.getPropertyIdsByTeam().get(teamId);
      if (propertyIds == null) {
        continue;
      }

      Instant now = clock.instant();
      for (UUID propertyId : propertyIds) {
        Unit unit =
            Unit.builder()
                .identifier(Optional.of(SidGenerator.newUnitId()))
                .teamId(teamId)
                .propertyId(propertyId)
                .unitNumber("1")
                .unitType(unitTypeForCategory(ctx.getPropertyCategory(propertyId)))
                .status(UnitStatus.VACANT)
                .implicit(true)
                .createdAt(Optional.of(now))
                .updatedAt(Optional.of(now))
                .createdBy(Optional.ofNullable(createdBy))
                .updatedBy(Optional.ofNullable(createdBy))
                .build();

        Unit saved = unitRepository.save(unit);
        ctx.putImplicitUnitId(propertyId, saved.getId());
      }

      log.info("Created {} implicit units for team {}", propertyIds.size(), teamKey);
    }
  }

  private UnitType unitTypeForCategory(String category) {
    return switch (category) {
      case "RESIDENTIAL", "MIXED_USE" -> UnitType.APARTMENT;
      default -> UnitType.COMMERCIAL;
    };
  }
}
