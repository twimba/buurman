package com.buurman.service.demo;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Unit;
import com.buurman.domain.UnitResidentialDetails;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.util.SidGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates the unit(s) for every demo property (BUUR-106). {@code contracts.unit_id}, {@code
 * property_occupancy_periods.unit_id} and {@code wws_calculations.unit_id} are all NOT NULL as of
 * V068, so every property generated for demo data needs at least one unit before contracts are
 * generated.
 *
 * <p>Most demo properties get a single "unit_number = 1" implicit unit, mirroring V068's own
 * backfill — {@link DemoContractGenerator} later flips it to OCCUPIED for properties with an active
 * contract. A handful of curated properties instead carry a {@link DemoDataContext.UnitPlan} list
 * (set by {@link DemoPropertyGenerator}), producing genuine multi-unit buildings with differing
 * floor areas, energy labels and bedroom counts — otherwise demo data would never exercise
 * unit-level features at all.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DemoUnitGenerator {

  private final UnitRepository unitRepository;
  private final UnitResidentialDetailsRepository unitResidentialDetailsRepository;
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
      int unitCount = 0;
      for (UUID propertyId : propertyIds) {
        List<DemoDataContext.UnitPlan> plans = ctx.getUnitPlans(propertyId);
        List<UUID> unitIds =
            plans.isEmpty()
                ? List.of(createImplicitUnit(ctx, teamId, propertyId, createdBy, now))
                : createPlannedUnits(ctx, teamId, propertyId, createdBy, now, plans);
        ctx.putUnitIds(propertyId, unitIds);
        unitCount += unitIds.size();
      }

      log.info("Created {} units for team {}", unitCount, teamKey);
    }
  }

  private UUID createImplicitUnit(
      DemoDataContext ctx, UUID teamId, UUID propertyId, @Nullable UUID createdBy, Instant now) {
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
    return unitRepository.save(unit).getId();
  }

  private List<UUID> createPlannedUnits(
      DemoDataContext ctx,
      UUID teamId,
      UUID propertyId,
      @Nullable UUID createdBy,
      Instant now,
      List<DemoDataContext.UnitPlan> plans) {
    List<UUID> unitIds = new ArrayList<>();
    int sortOrder = 0;
    for (DemoDataContext.UnitPlan plan : plans) {
      Unit unit =
          Unit.builder()
              .identifier(Optional.of(SidGenerator.newUnitId()))
              .teamId(teamId)
              .propertyId(propertyId)
              .unitNumber(plan.unitNumber())
              .sortOrder(sortOrder++)
              .unitType(plan.unitType())
              .status(UnitStatus.VACANT)
              .implicit(false)
              .areaValue(Optional.of(plan.areaSqm()))
              .energyEfficiencyRating(Optional.ofNullable(plan.energyLabel()))
              .createdAt(Optional.of(now))
              .updatedAt(Optional.of(now))
              .createdBy(Optional.ofNullable(createdBy))
              .updatedBy(Optional.ofNullable(createdBy))
              .build();
      UUID unitId = unitRepository.save(unit).getId();
      unitIds.add(unitId);

      if (plan.unitType() == UnitType.APARTMENT) {
        unitResidentialDetailsRepository.save(
            UnitResidentialDetails.builder()
                .unitId(unitId)
                .teamId(teamId)
                .bedrooms(Optional.of(plan.bedrooms()))
                .bathrooms(Optional.of(plan.bathrooms()))
                .furnished(plan.furnished())
                .createdBy(Optional.ofNullable(createdBy))
                .updatedBy(Optional.ofNullable(createdBy))
                .build());
      }
    }
    return unitIds;
  }

  private UnitType unitTypeForCategory(String category) {
    return switch (category) {
      case "RESIDENTIAL", "MIXED_USE" -> UnitType.APARTMENT;
      default -> UnitType.COMMERCIAL;
    };
  }
}
