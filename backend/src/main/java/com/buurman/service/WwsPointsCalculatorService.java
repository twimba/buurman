package com.buurman.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.Property;
import com.buurman.domain.PropertyOutdoorArea;
import com.buurman.domain.PropertyResidentialDetails;
import com.buurman.domain.Sid;
import com.buurman.domain.WwsCalculation;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.WwsCalculationRequest;
import com.buurman.dto.response.WwsCalculationResponse;
import com.buurman.dto.response.WwsCategoryBreakdown;
import com.buurman.dto.response.WwsPreFillResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyResidentialDetailsRepository;
import com.buurman.repository.WwsCalculationRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.SidGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WwsPointsCalculatorService {

  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyResidentialDetailsRepository residentialDetailsRepository;
  private final PropertyOutdoorAreaRepository outdoorAreaRepository;
  private final WwsCalculationRepository wwsCalculationRepository;
  private final ObjectMapper objectMapper;

  // 2024 energy label points lookup
  private static final Map<String, BigDecimal> ENERGY_LABEL_POINTS_2024 =
      Map.ofEntries(
          Map.entry("A+++++", new BigDecimal("52")),
          Map.entry("A++++", new BigDecimal("48")),
          Map.entry("A+++", new BigDecimal("44")),
          Map.entry("A++", new BigDecimal("40")),
          Map.entry("A+", new BigDecimal("36")),
          Map.entry("A", new BigDecimal("32")),
          Map.entry("B", new BigDecimal("22")),
          Map.entry("C", new BigDecimal("15")),
          Map.entry("D", new BigDecimal("11")),
          Map.entry("E", new BigDecimal("5")),
          Map.entry("F", new BigDecimal("1")),
          Map.entry("G", new BigDecimal("-5")));

  // Parking type points
  private static final Map<String, BigDecimal> PARKING_POINTS =
      Map.of(
          "GARAGE", new BigDecimal("9"),
          "CARPORT", new BigDecimal("6"),
          "DESIGNATED_SPOT", new BigDecimal("4"),
          "STREET", BigDecimal.ZERO,
          "NONE", BigDecimal.ZERO);

  // Max rent lookup (2024 regulated sector, simplified table for common ranges)
  private static final BigDecimal MAX_RENT_DIVIDER = new BigDecimal("92870");

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public WwsCalculationResponse calculate(WwsCalculationRequest request, UserPrincipal principal) {
    List<WwsCategoryBreakdown> breakdown = calculateBreakdown(request);
    BigDecimal totalPoints =
        breakdown.stream()
            .map(WwsCategoryBreakdown::points)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

    String classification = classifyPoints(totalPoints);
    Optional<BigDecimal> maxRent = calculateMaxRent(totalPoints, classification);

    return new WwsCalculationResponse(
        Optional.empty(),
        totalPoints,
        classification,
        maxRent,
        request.systemVersion(),
        LocalDate.now(),
        breakdown);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public WwsCalculationResponse calculateAndSave(
      WwsCalculationRequest request, UserPrincipal principal) {
    UUID teamId = principal.getTeamId().orElseThrow();
    UUID userId = principal.getUserId();

    Property property =
        propertyRepository.getByIdentifierAndTeamId(request.propertyIdentifier(), teamId);

    List<WwsCategoryBreakdown> breakdown = calculateBreakdown(request);
    BigDecimal totalPoints =
        breakdown.stream()
            .map(WwsCategoryBreakdown::points)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

    String classification = classifyPoints(totalPoints);
    Optional<BigDecimal> maxRent = calculateMaxRent(totalPoints, classification);

    Sid identifier = SidGenerator.newWwsCalculationId();

    String inputJson;
    String breakdownJson;
    try {
      inputJson = objectMapper.writeValueAsString(request);
      breakdownJson =
          objectMapper.writeValueAsString(
              breakdown.stream()
                  .map(
                      b ->
                          new WwsCalculation.CategoryBreakdown(
                              b.key(), b.name(), b.nameNl(), b.points(), b.explanation()))
                  .toList());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize WWS data", e);
    }

    WwsCalculation calc =
        WwsCalculation.builder()
            .identifier(Optional.of(identifier))
            .teamId(teamId)
            .propertyId(property.getId())
            .contractId(
                request.contractIdentifier().map(ci -> resolveContractId(ci, teamId)))
            .systemVersion(request.systemVersion())
            .totalPoints(totalPoints)
            .sectorClassification(classification)
            .maxRentIndication(maxRent)
            .categoryBreakdown(
                breakdown.stream()
                    .map(
                        b ->
                            new WwsCalculation.CategoryBreakdown(
                                b.key(), b.name(), b.nameNl(), b.points(), b.explanation()))
                    .toList())
            .breakdownJson(breakdownJson)
            .inputDataJson(inputJson)
            .calculationDate(LocalDate.now())
            .createdBy(userId)
            .updatedBy(userId)
            .build();

    wwsCalculationRepository.save(calc);

    return new WwsCalculationResponse(
        Optional.of(identifier),
        totalPoints,
        classification,
        maxRent,
        request.systemVersion(),
        calc.getCalculationDate(),
        breakdown);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public WwsPreFillResponse getPreFillData(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.getTeamId().orElseThrow();
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    Optional<PropertyResidentialDetails> residential =
        residentialDetailsRepository.findByPropertyIdAndTeamId(property.getId(), teamId);

    List<PropertyOutdoorArea> outdoorAreas =
        outdoorAreaRepository.findByPropertyIdAndTeamId(property.getId(), teamId);

    // Surface area (only if in sqm)
    Optional<BigDecimal> surfaceArea =
        property
            .getAreaUnit()
            .filter(unit -> "SQM".equalsIgnoreCase(unit) || "M2".equalsIgnoreCase(unit))
            .flatMap(unit -> property.getAreaValue());

    // Rooms from residential details (bedrooms as proxy)
    Optional<Integer> rooms = residential.flatMap(PropertyResidentialDetails::getBedrooms);

    // Energy label
    Optional<String> energyLabel = property.getEnergyEfficiencyRating();

    // Outdoor space total (sum all outdoor areas in sqm)
    BigDecimal outdoorTotal =
        outdoorAreas.stream()
            .filter(
                oa ->
                    "SQM".equalsIgnoreCase(oa.getAreaUnit())
                        || "M2".equalsIgnoreCase(oa.getAreaUnit()))
            .map(oa -> oa.getAreaValue().orElse(BigDecimal.ZERO))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Parking
    Optional<String> parkingType = property.getParkingType();
    Optional<Integer> parkingSpaces = property.getParkingSpaces();

    // Accessibility features count
    int accessibilityCount = 0;
    if (property.getIsWheelchairAccessible().orElse(false)) {
      accessibilityCount++;
    }
    if (property.getHasElevator().orElse(false)) {
      accessibilityCount++;
    }
    if (property.getHasStepFreeEntrance().orElse(false)) {
      accessibilityCount++;
    }
    if (property.getHasAdaptedBathroom().orElse(false)) {
      accessibilityCount++;
    }

    // Property address
    String address = property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity();

    return new WwsPreFillResponse(
        surfaceArea,
        rooms,
        Optional.empty(), // heated rooms not stored separately
        energyLabel,
        outdoorTotal.compareTo(BigDecimal.ZERO) > 0
            ? Optional.of(outdoorTotal)
            : Optional.empty(),
        parkingType,
        parkingSpaces,
        accessibilityCount > 0 ? Optional.of(accessibilityCount) : Optional.empty(),
        Optional.of(address));
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public List<WwsCalculationResponse> getCalculationHistory(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.getTeamId().orElseThrow();
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    return wwsCalculationRepository.findByPropertyId(property.getId(), teamId).stream()
        .map(this::toResponse)
        .toList();
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public WwsCalculationResponse getLatestCalculation(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.getTeamId().orElseThrow();
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    return wwsCalculationRepository
        .findLatestByPropertyId(property.getId(), teamId)
        .map(this::toResponse)
        .orElseThrow(() -> new NotFoundException("No WWS calculation found for this property"));
  }

  private List<WwsCategoryBreakdown> calculateBreakdown(WwsCalculationRequest request) {
    List<WwsCategoryBreakdown> breakdown = new ArrayList<>();
    boolean is2024 = "2024".equals(request.systemVersion());

    // 1. Surface Area (Oppervlakte) — 1 pt/m² for 2024
    BigDecimal surfacePoints =
        request
            .surfaceAreaSqm()
            .map(area -> area.setScale(2, RoundingMode.HALF_UP))
            .orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "SURFACE_AREA",
            "Surface Area",
            "Oppervlakte",
            surfacePoints,
            surfacePoints.compareTo(BigDecimal.ZERO) > 0
                ? surfacePoints + " m² x 1 pt = " + surfacePoints + " pts"
                : "Not provided"));

    // 2. Rooms (Kamers) — 2 pts/room
    BigDecimal roomPoints =
        request
            .numberOfRooms()
            .map(r -> new BigDecimal(r * 2))
            .orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "ROOMS",
            "Rooms",
            "Kamers",
            roomPoints,
            request
                .numberOfRooms()
                .map(r -> r + " rooms x 2 pts = " + roomPoints + " pts")
                .orElse("Not provided")));

    // 3. Heating (Verwarming) — 2 pts/heated room
    BigDecimal heatingPoints =
        request
            .numberOfHeatedRooms()
            .map(r -> new BigDecimal(r * 2))
            .orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "HEATING",
            "Heating",
            "Verwarming",
            heatingPoints,
            request
                .numberOfHeatedRooms()
                .map(r -> r + " heated rooms x 2 pts = " + heatingPoints + " pts")
                .orElse("Not provided")));

    // 4. Energy Label (Energielabel)
    BigDecimal energyPoints =
        request
            .energyLabel()
            .map(label -> ENERGY_LABEL_POINTS_2024.getOrDefault(label.toUpperCase(), BigDecimal.ZERO))
            .orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "ENERGY_LABEL",
            "Energy Label",
            "Energielabel",
            energyPoints,
            request
                .energyLabel()
                .map(label -> "Label " + label + " = " + energyPoints + " pts")
                .orElse("Not provided")));

    // 5. Kitchen (Keuken) — direct quality points (0-15)
    BigDecimal kitchenPoints =
        request.kitchenQualityPoints().orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "KITCHEN",
            "Kitchen",
            "Keuken",
            kitchenPoints,
            kitchenPoints.compareTo(BigDecimal.ZERO) > 0
                ? "Kitchen quality: " + kitchenPoints + " pts"
                : "Not assessed"));

    // 6. Bathroom (Sanitair) — direct quality points (0-15)
    BigDecimal bathroomPoints =
        request.bathroomQualityPoints().orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "BATHROOM",
            "Bathroom",
            "Sanitair",
            bathroomPoints,
            bathroomPoints.compareTo(BigDecimal.ZERO) > 0
                ? "Bathroom quality: " + bathroomPoints + " pts"
                : "Not assessed"));

    // 7. WOZ Value (WOZ-waarde)
    BigDecimal wozPoints =
        request
            .wozValue()
            .filter(woz -> woz.compareTo(BigDecimal.ZERO) > 0)
            .map(
                woz -> {
                  // Points = WOZ / divider (2024: €92,870 per point)
                  return woz.divide(MAX_RENT_DIVIDER, 2, RoundingMode.HALF_UP);
                })
            .orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "WOZ_VALUE",
            "WOZ Value",
            "WOZ-waarde",
            wozPoints,
            request
                .wozValue()
                .filter(woz -> woz.compareTo(BigDecimal.ZERO) > 0)
                .map(
                    woz ->
                        "€"
                            + woz.setScale(0, RoundingMode.HALF_UP)
                            + " / €"
                            + MAX_RENT_DIVIDER.setScale(0, RoundingMode.HALF_UP)
                            + " = "
                            + wozPoints
                            + " pts")
                .orElse("Not provided")));

    // 8. Outdoor Space (Buitenruimte) — 2 pts per 25m²
    BigDecimal outdoorPoints =
        request
            .outdoorSpaceSqm()
            .filter(sqm -> sqm.compareTo(BigDecimal.ZERO) > 0)
            .map(sqm -> sqm.divide(new BigDecimal("25"), 0, RoundingMode.DOWN).multiply(new BigDecimal("2")))
            .orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "OUTDOOR_SPACE",
            "Outdoor Space",
            "Buitenruimte",
            outdoorPoints,
            request
                .outdoorSpaceSqm()
                .filter(sqm -> sqm.compareTo(BigDecimal.ZERO) > 0)
                .map(sqm -> sqm + " m² / 25 x 2 = " + outdoorPoints + " pts")
                .orElse("Not provided")));

    // 9. Parking (Parkeren)
    BigDecimal parkingBasePoints =
        request
            .parkingType()
            .map(type -> PARKING_POINTS.getOrDefault(type.toUpperCase(), BigDecimal.ZERO))
            .orElse(BigDecimal.ZERO);
    int spaces = request.parkingSpaces().orElse(1);
    BigDecimal parkingPoints = parkingBasePoints.multiply(new BigDecimal(Math.max(spaces, 1)));
    breakdown.add(
        new WwsCategoryBreakdown(
            "PARKING",
            "Parking",
            "Parkeren",
            parkingPoints,
            request
                .parkingType()
                .map(
                    type ->
                        type
                            + " x "
                            + spaces
                            + " = "
                            + parkingPoints
                            + " pts")
                .orElse("Not provided")));

    // 10. Location (Ligging) — direct bonus/penalty (-10 to +15)
    BigDecimal locationPoints =
        request.locationBonus().orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "LOCATION",
            "Location",
            "Ligging",
            locationPoints,
            locationPoints.compareTo(BigDecimal.ZERO) != 0
                ? "Location adjustment: " + locationPoints + " pts"
                : "No adjustment"));

    // 11. Renovation (Renovatie) — 0.2 pts per €1,000 invested
    BigDecimal renovationPoints =
        request
            .renovationInvestment()
            .filter(inv -> inv.compareTo(BigDecimal.ZERO) > 0)
            .map(
                inv ->
                    inv.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("0.2"))
                        .setScale(2, RoundingMode.HALF_UP))
            .orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "RENOVATION",
            "Renovation",
            "Renovatie",
            renovationPoints,
            request
                .renovationInvestment()
                .filter(inv -> inv.compareTo(BigDecimal.ZERO) > 0)
                .map(
                    inv ->
                        "€"
                            + inv.setScale(0, RoundingMode.HALF_UP)
                            + " / €1000 x 0.2 = "
                            + renovationPoints
                            + " pts")
                .orElse("Not provided")));

    // 12. Accessibility (Toegankelijkheid) — 3 pts per feature
    BigDecimal accessibilityPoints =
        request
            .accessibilityFeatures()
            .filter(f -> f > 0)
            .map(f -> new BigDecimal(f * 3))
            .orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "ACCESSIBILITY",
            "Accessibility",
            "Toegankelijkheid",
            accessibilityPoints,
            request
                .accessibilityFeatures()
                .filter(f -> f > 0)
                .map(f -> f + " features x 3 = " + accessibilityPoints + " pts")
                .orElse("Not provided")));

    // 13. Common Areas (Gemeenschappelijke ruimten) — 0.75 pt per m²
    BigDecimal commonAreaPoints =
        request
            .commonAreaSqm()
            .filter(sqm -> sqm.compareTo(BigDecimal.ZERO) > 0)
            .map(
                sqm ->
                    sqm.multiply(new BigDecimal("0.75"))
                        .setScale(2, RoundingMode.HALF_UP))
            .orElse(BigDecimal.ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "COMMON_AREAS",
            "Common Areas",
            "Gemeenschappelijke ruimten",
            commonAreaPoints,
            request
                .commonAreaSqm()
                .filter(sqm -> sqm.compareTo(BigDecimal.ZERO) > 0)
                .map(sqm -> sqm + " m² x 0.75 = " + commonAreaPoints + " pts")
                .orElse("Not provided")));

    return breakdown;
  }

  private String classifyPoints(BigDecimal totalPoints) {
    if (totalPoints.compareTo(new BigDecimal("186")) <= 0) {
      return "REGULATED";
    } else if (totalPoints.compareTo(new BigDecimal("250")) <= 0) {
      return "MID_SEGMENT";
    } else {
      return "FREE_SECTOR";
    }
  }

  private Optional<BigDecimal> calculateMaxRent(BigDecimal totalPoints, String classification) {
    if ("FREE_SECTOR".equals(classification)) {
      return Optional.empty(); // Free sector has no maximum rent
    }
    // Simplified 2024 max rent: base €5.44/point for regulated sector
    BigDecimal baseRate = new BigDecimal("5.44");
    return Optional.of(
        totalPoints.multiply(baseRate).setScale(2, RoundingMode.HALF_UP));
  }

  private WwsCalculationResponse toResponse(WwsCalculation calc) {
    return new WwsCalculationResponse(
        calc.getIdentifier(),
        calc.getTotalPoints(),
        calc.getSectorClassification(),
        calc.getMaxRentIndication(),
        calc.getSystemVersion(),
        calc.getCalculationDate(),
        calc.getCategoryBreakdown().stream()
            .map(
                b ->
                    new WwsCategoryBreakdown(
                        b.key(), b.name(), b.nameNl(), b.points(), b.explanation()))
            .toList());
  }

  private UUID resolveContractId(String contractIdentifier, UUID teamId) {
    return contractRepository
        .findByIdentifierAndTeamId(Sid.of(contractIdentifier), teamId)
        .map(c -> c.getId())
        .orElseThrow(() -> new NotFoundException("Contract not found: " + contractIdentifier));
  }
}
