package com.buurman.service.export.tabular;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;

import lombok.RequiredArgsConstructor;

/** Single-sheet TabularExport for all team properties. */
@Component
@RequiredArgsConstructor
public class PropertyTabularExportBuilder {

  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;

  public TabularExport build(UUID teamId) {
    List<Property> properties = propertyRepository.findAllByTeamId(teamId);
    Map<UUID, List<Unit>> unitsByProperty =
        unitRepository.findAllByTeamId(teamId).stream()
            .collect(Collectors.groupingBy(Unit::getPropertyId));

    // Status, Area, Energy Rating and Heating moved from properties to units in V068: a property
    // now has N units, so each column is an aggregate across them. A single-unit property (the
    // overwhelming majority, backfilled as one implicit unit) degenerates to that one unit's raw
    // value, reading exactly as it did before the migration.
    TabularExport export = new TabularExport("properties");
    TabularSheet sheet =
        export.addSheet(
            "Properties",
            List.of(
                TabularColumn.text("Identifier"),
                TabularColumn.text("Street"),
                TabularColumn.text("City"),
                TabularColumn.text("Postal Code"),
                TabularColumn.text("Country"),
                TabularColumn.text("Region"),
                TabularColumn.text("Category"),
                TabularColumn.text("Type"),
                TabularColumn.text("Status"),
                TabularColumn.of("Year Built", TabularColumnFormat.INTEGER),
                TabularColumn.of("Year Last Renovated", TabularColumnFormat.INTEGER),
                TabularColumn.of("Floors", TabularColumnFormat.INTEGER),
                TabularColumn.of("Total Area (sqm)", TabularColumnFormat.NUMBER),
                TabularColumn.text("Energy Rating"),
                TabularColumn.text("Heating"),
                TabularColumn.of("Parking Spaces", TabularColumnFormat.INTEGER),
                TabularColumn.text("Has Elevator"),
                TabularColumn.text("Wheelchair Accessible"),
                TabularColumn.text("Created At"),
                TabularColumn.text("Updated At")));

    for (Property p : properties) {
      List<Unit> units = unitsByProperty.getOrDefault(p.getId(), List.of());
      sheet.addRow(
          p.getIdentifier().map(Object::toString).orElse(""),
          p.getStreet(),
          p.getCity(),
          p.getPostalCode(),
          p.getCountryCode(),
          p.getRegionCode().orElse(""),
          p.getPropertyCategory().name(),
          p.getPropertyType().name(),
          statusSummary(units),
          p.getYearBuilt().isPresent() ? p.getYearBuilt().get() : "",
          p.getYearLastRenovated().isPresent() ? p.getYearLastRenovated().get() : "",
          p.getNumberOfFloors().isPresent() ? p.getNumberOfFloors().get() : "",
          totalArea(units),
          commonOrMixed(units, Unit::getEnergyEfficiencyRating),
          commonOrMixed(units, Unit::getHeatingType),
          p.getParkingSpaces().isPresent() ? p.getParkingSpaces().get() : "",
          p.getHasElevator().map(b -> b ? "Yes" : "No").orElse(""),
          p.getIsWheelchairAccessible().map(b -> b ? "Yes" : "No").orElse(""),
          p.getCreatedAt() != null ? p.getCreatedAt().toString() : "",
          p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
    }
    return export;
  }

  /**
   * A single unit reports its own status verbatim (matching pre-migration behaviour). Several units
   * report an occupancy fraction ("8/10 OCCUPIED") — the aggregate a landlord scanning a portfolio
   * export actually wants, rather than one arbitrary unit's status.
   */
  private String statusSummary(List<Unit> units) {
    if (units.isEmpty()) {
      return "";
    }
    if (units.size() == 1) {
      return units.get(0).getStatus().name();
    }
    long occupied =
        units.stream()
            .filter(
                u ->
                    u.getStatus() == UnitStatus.OCCUPIED
                        || u.getStatus() == UnitStatus.SELF_OCCUPIED)
            .count();
    return occupied + "/" + units.size() + " OCCUPIED";
  }

  /** Sum of every unit's floor area. Degenerates to the one unit's own area when there is one. */
  private Object totalArea(List<Unit> units) {
    BigDecimal sum = BigDecimal.ZERO;
    boolean any = false;
    for (Unit u : units) {
      if (u.getAreaValue().isPresent()) {
        sum = sum.add(u.getAreaValue().get());
        any = true;
      }
    }
    return any ? sum : "";
  }

  /**
   * A qualitative per-unit attribute (energy rating, heating type) that cannot be summed: reports
   * the shared value when every unit that has one agrees, "MIXED" when they differ, and "" when no
   * unit carries the attribute. A single unit degenerates to its own raw value.
   */
  private String commonOrMixed(List<Unit> units, Function<Unit, Optional<String>> extractor) {
    List<String> values =
        units.stream()
            .map(extractor)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .toList();
    if (values.isEmpty()) {
      return "";
    }
    return values.size() == 1 ? values.get(0) : "MIXED";
  }
}
