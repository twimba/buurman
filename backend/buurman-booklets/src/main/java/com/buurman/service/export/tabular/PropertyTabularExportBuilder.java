package com.buurman.service.export.tabular;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Property;
import com.buurman.repository.PropertyRepository;

import lombok.RequiredArgsConstructor;

/** Single-sheet TabularExport for all team properties. */
@Component
@RequiredArgsConstructor
public class PropertyTabularExportBuilder {

  private final PropertyRepository propertyRepository;

  public TabularExport build(UUID teamId) {
    List<Property> properties = propertyRepository.findAllByTeamId(teamId);

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
                TabularColumn.of("Area", TabularColumnFormat.NUMBER),
                TabularColumn.text("Area Unit"),
                TabularColumn.of("Year Built", TabularColumnFormat.INTEGER),
                TabularColumn.of("Year Last Renovated", TabularColumnFormat.INTEGER),
                TabularColumn.text("Energy Rating"),
                TabularColumn.of("Floors", TabularColumnFormat.INTEGER),
                TabularColumn.of("Parking Spaces", TabularColumnFormat.INTEGER),
                TabularColumn.text("Heating"),
                TabularColumn.text("Has Elevator"),
                TabularColumn.text("Wheelchair Accessible"),
                TabularColumn.text("Created At"),
                TabularColumn.text("Updated At")));

    for (Property p : properties) {
      sheet.addRow(
          p.getIdentifier().map(Object::toString).orElse(""),
          p.getStreet(),
          p.getCity(),
          p.getPostalCode(),
          p.getCountryCode(),
          p.getRegionCode().orElse(""),
          p.getPropertyCategory().name(),
          p.getPropertyType().name(),
          p.getStatus().name(),
          p.getAreaValue().isPresent() ? p.getAreaValue().get() : "",
          p.getAreaUnit().orElse(""),
          p.getYearBuilt().isPresent() ? p.getYearBuilt().get() : "",
          p.getYearLastRenovated().isPresent() ? p.getYearLastRenovated().get() : "",
          p.getEnergyEfficiencyRating().orElse(""),
          p.getNumberOfFloors().isPresent() ? p.getNumberOfFloors().get() : "",
          p.getParkingSpaces().isPresent() ? p.getParkingSpaces().get() : "",
          p.getHeatingType().orElse(""),
          p.getHasElevator().map(b -> b ? "Yes" : "No").orElse(""),
          p.getIsWheelchairAccessible().map(b -> b ? "Yes" : "No").orElse(""),
          p.getCreatedAt() != null ? p.getCreatedAt().toString() : "",
          p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : "");
    }
    return export;
  }
}
