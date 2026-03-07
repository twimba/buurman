package com.buurman.service;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.DOWN;
import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyOutdoorArea;
import com.buurman.domain.PropertyResidentialDetails;
import com.buurman.domain.Sid;
import com.buurman.domain.WwsCalculation;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.WwsCalculationIdentifier;
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

  private final Clock clock;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyResidentialDetailsRepository residentialDetailsRepository;
  private final PropertyOutdoorAreaRepository outdoorAreaRepository;
  private final WwsCalculationRepository wwsCalculationRepository;
  private final ObjectMapper objectMapper;

  // --- Year-specific version configuration ---

  private record WwsVersionConfig(
      Map<String, BigDecimal> energyLabelPoints,
      BigDecimal wozDividerI,
      BigDecimal wozDividerII,
      BigDecimal wozMinimum,
      boolean hasWozCap,
      boolean hasMidSegment,
      int regulatedMaxPoints,
      int midSegmentMaxPoints,
      BigDecimal socialThresholdRent,
      BigDecimal midSegmentThresholdRent,
      boolean newOutdoorFormula,
      boolean hasRenovation) {}

  // Pre-July 2024 energy label points
  private static final Map<String, BigDecimal> ENERGY_LABELS_PRE_2024 =
      Map.ofEntries(
          Map.entry("A++++", new BigDecimal("48")),
          Map.entry("A+++", new BigDecimal("44")),
          Map.entry("A++", new BigDecimal("40")),
          Map.entry("A+", new BigDecimal("36")),
          Map.entry("A", new BigDecimal("32")),
          Map.entry("B", new BigDecimal("22")),
          Map.entry("C", new BigDecimal("15")),
          Map.entry("D", new BigDecimal("11")),
          Map.entry("E", new BigDecimal("8")),
          Map.entry("F", new BigDecimal("4")),
          Map.entry("G", ZERO));

  // Post-July 2024 (Wet betaalbare huur) energy label points
  private static final Map<String, BigDecimal> ENERGY_LABELS_POST_2024 =
      Map.ofEntries(
          Map.entry("A++++", new BigDecimal("62")),
          Map.entry("A+++", new BigDecimal("57")),
          Map.entry("A++", new BigDecimal("52")),
          Map.entry("A+", new BigDecimal("47")),
          Map.entry("A", new BigDecimal("41")),
          Map.entry("B", new BigDecimal("34")),
          Map.entry("C", new BigDecimal("22")),
          Map.entry("D", new BigDecimal("14")),
          Map.entry("E", new BigDecimal("-4")),
          Map.entry("F", new BigDecimal("-9")),
          Map.entry("G", new BigDecimal("-15")));

  private static final BigDecimal RENOVATION_DIVISOR = new BigDecimal("332");
  private static final BigDecimal WOZ_CAP_RATIO = new BigDecimal("0.33");

  private static final Map<String, WwsVersionConfig> VERSION_CONFIGS =
      Map.of(
          "2023",
          new WwsVersionConfig(
              ENERGY_LABELS_PRE_2024,
              new BigDecimal("14543"),
              new BigDecimal("229"),
              new BigDecimal("73607"),
              false,
              false,
              141,
              0,
              new BigDecimal("808.06"),
              ZERO,
              false,
              false),
          "2024",
          new WwsVersionConfig(
              ENERGY_LABELS_POST_2024,
              new BigDecimal("15329"),
              new BigDecimal("242"),
              new BigDecimal("77582"),
              true,
              true,
              143,
              186,
              new BigDecimal("879.66"),
              new BigDecimal("1184.82"),
              true,
              true),
          "2025",
          new WwsVersionConfig(
              ENERGY_LABELS_POST_2024,
              new BigDecimal("16954"),
              new BigDecimal("268"),
              new BigDecimal("85806"),
              true,
              true,
              143,
              186,
              new BigDecimal("879.66"),
              new BigDecimal("1184.82"),
              true,
              true),
          "2026",
          new WwsVersionConfig(
              ENERGY_LABELS_POST_2024,
              new BigDecimal("16954"),
              new BigDecimal("268"),
              new BigDecimal("85806"),
              true,
              true,
              143,
              186,
              new BigDecimal("932.93"),
              new BigDecimal("1228.07"),
              true,
              true));

  private static final Map<String, BigDecimal> PARKING_POINTS =
      Map.of(
          "GARAGE", new BigDecimal("9"),
          "CARPORT", new BigDecimal("6"),
          "DESIGNATED_SPOT", new BigDecimal("4"),
          "STREET", ZERO,
          "NONE", ZERO);

  private static WwsVersionConfig getVersionConfig(String systemVersion) {
    WwsVersionConfig config = VERSION_CONFIGS.get(systemVersion);
    if (config == null) {
      throw new IllegalArgumentException("Unsupported WWS system version: " + systemVersion);
    }
    return config;
  }

  // --- Public API ---

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public WwsCalculationResponse calculate(WwsCalculationRequest request, UserPrincipal principal) {
    WwsVersionConfig config = getVersionConfig(request.systemVersion());
    List<WwsCategoryBreakdown> breakdown = calculateBreakdown(request, config);
    BigDecimal totalPoints = sumPoints(breakdown);

    String classification = classifyPoints(totalPoints, config);
    Optional<BigDecimal> maxRent = calculateMaxRent(totalPoints, classification, config);

    return new WwsCalculationResponse(
        Optional.empty(),
        totalPoints,
        classification,
        maxRent,
        request.systemVersion(),
        LocalDate.now(clock),
        breakdown,
        Optional.empty());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public WwsCalculationResponse calculateAndSave(
      WwsCalculationRequest request, UserPrincipal principal) {
    UUID teamId = principal.getTeamId().orElseThrow();
    UUID userId = principal.getUserId();

    Property property =
        propertyRepository.getByIdentifierAndTeamId(request.propertyIdentifier(), teamId);

    WwsVersionConfig config = getVersionConfig(request.systemVersion());
    List<WwsCategoryBreakdown> breakdown = calculateBreakdown(request, config);
    BigDecimal totalPoints = sumPoints(breakdown);

    String classification = classifyPoints(totalPoints, config);
    Optional<BigDecimal> maxRent = calculateMaxRent(totalPoints, classification, config);

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
            .contractId(request.contractIdentifier().map(ci -> resolveContractId(ci, teamId)))
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
            .calculationDate(LocalDate.now(clock))
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
        breakdown,
        Optional.of(request));
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public WwsPreFillResponse getPreFillData(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.getTeamId().orElseThrow();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    Optional<PropertyResidentialDetails> residential =
        residentialDetailsRepository.findByPropertyIdAndTeamId(property.getId(), teamId);

    List<PropertyOutdoorArea> outdoorAreas =
        outdoorAreaRepository.findByPropertyIdAndTeamId(property.getId(), teamId);

    Optional<BigDecimal> surfaceArea =
        property
            .getAreaUnit()
            .filter(unit -> "SQM".equalsIgnoreCase(unit) || "M2".equalsIgnoreCase(unit))
            .flatMap(unit -> property.getAreaValue());

    Optional<Integer> rooms = residential.flatMap(PropertyResidentialDetails::getBedrooms);

    Optional<String> energyLabel = property.getEnergyEfficiencyRating();

    BigDecimal outdoorTotal =
        outdoorAreas.stream()
            .filter(
                oa ->
                    "SQM".equalsIgnoreCase(oa.getAreaUnit())
                        || "M2".equalsIgnoreCase(oa.getAreaUnit()))
            .map(oa -> oa.getAreaValue().orElse(ZERO))
            .reduce(ZERO, BigDecimal::add);

    Optional<String> parkingType = property.getParkingType();
    Optional<Integer> parkingSpaces = property.getParkingSpaces();

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

    String address =
        property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity();

    return new WwsPreFillResponse(
        surfaceArea,
        rooms,
        Optional.empty(),
        energyLabel,
        outdoorTotal.compareTo(ZERO) > 0 ? Optional.of(outdoorTotal) : Optional.empty(),
        parkingType,
        parkingSpaces,
        accessibilityCount > 0 ? Optional.of(accessibilityCount) : Optional.empty(),
        Optional.of(address));
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public List<WwsCalculationResponse> getCalculationHistory(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.getTeamId().orElseThrow();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    return wwsCalculationRepository.findByPropertyId(property.getId(), teamId).stream()
        .map(this::toResponse)
        .toList();
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public WwsCalculationResponse getLatestCalculation(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.getTeamId().orElseThrow();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    return wwsCalculationRepository
        .findLatestByPropertyId(property.getId(), teamId)
        .map(this::toResponse)
        .orElseThrow(() -> new NotFoundException("No WWS calculation found for this property"));
  }

  // --- Calculation logic ---

  private static BigDecimal sumPoints(List<WwsCategoryBreakdown> breakdown) {
    return breakdown.stream()
        .map(WwsCategoryBreakdown::points)
        .reduce(ZERO, BigDecimal::add)
        .setScale(2, HALF_UP);
  }

  private List<WwsCategoryBreakdown> calculateBreakdown(
      WwsCalculationRequest request, WwsVersionConfig config) {
    List<WwsCategoryBreakdown> breakdown = new ArrayList<>();

    // 1. Surface Area (Oppervlakte) — 1 pt/m²
    BigDecimal surfacePoints =
        request.surfaceAreaSqm().map(area -> area.setScale(2, HALF_UP)).orElse(ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "SURFACE_AREA",
            "Surface Area",
            "Oppervlakte",
            surfacePoints,
            surfacePoints.compareTo(ZERO) > 0
                ? surfacePoints + " m² × 1 pt = " + surfacePoints + " pts"
                : "Not provided"));

    // 2. Rooms (Kamers) — 2 pts/room
    BigDecimal roomPoints = request.numberOfRooms().map(r -> new BigDecimal(r * 2)).orElse(ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "ROOMS",
            "Rooms",
            "Kamers",
            roomPoints,
            request
                .numberOfRooms()
                .map(r -> r + " rooms × 2 pts = " + roomPoints + " pts")
                .orElse("Not provided")));

    // 3. Heating (Verwarming) — 2 pts/heated room
    BigDecimal heatingPoints =
        request.numberOfHeatedRooms().map(r -> new BigDecimal(r * 2)).orElse(ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "HEATING",
            "Heating",
            "Verwarming",
            heatingPoints,
            request
                .numberOfHeatedRooms()
                .map(r -> r + " heated rooms × 2 pts = " + heatingPoints + " pts")
                .orElse("Not provided")));

    // 4. Energy Label (Energielabel) — version-specific points
    BigDecimal energyPoints =
        request
            .energyLabel()
            .map(
                label ->
                    config.energyLabelPoints().getOrDefault(label.toUpperCase(Locale.ROOT), ZERO))
            .orElse(ZERO);
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

    // 5. Kitchen (Keuken) — direct quality points
    BigDecimal kitchenPoints = request.kitchenQualityPoints().orElse(ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "KITCHEN",
            "Kitchen",
            "Keuken",
            kitchenPoints,
            kitchenPoints.compareTo(ZERO) > 0
                ? "Kitchen quality: " + kitchenPoints + " pts"
                : "Not assessed"));

    // 6. Bathroom (Sanitair) — direct quality points
    BigDecimal bathroomPoints = request.bathroomQualityPoints().orElse(ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "BATHROOM",
            "Bathroom",
            "Sanitair",
            bathroomPoints,
            bathroomPoints.compareTo(ZERO) > 0
                ? "Bathroom quality: " + bathroomPoints + " pts"
                : "Not assessed"));

    // 7. WOZ Value (WOZ-waarde) — 2-component formula with year-specific divisors
    BigDecimal rawWozPoints = calculateWozPoints(request, config);
    String wozExplanation = buildWozExplanation(request, config, rawWozPoints);
    int wozIndex = breakdown.size();
    breakdown.add(
        new WwsCategoryBreakdown(
            "WOZ_VALUE", "WOZ Value", "WOZ-waarde", rawWozPoints, wozExplanation));

    // 8. Outdoor Space (Buitenruimte) — version-specific formula
    BigDecimal outdoorPoints = calculateOutdoorPoints(request, config);
    breakdown.add(
        new WwsCategoryBreakdown(
            "OUTDOOR_SPACE",
            "Outdoor Space",
            "Buitenruimte",
            outdoorPoints,
            buildOutdoorExplanation(request, config, outdoorPoints)));

    // 9. Parking (Parkeren)
    BigDecimal parkingBasePoints =
        request
            .parkingType()
            .map(type -> PARKING_POINTS.getOrDefault(type.toUpperCase(Locale.ROOT), ZERO))
            .orElse(ZERO);
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
                .map(type -> type + " × " + spaces + " = " + parkingPoints + " pts")
                .orElse("Not provided")));

    // 10. Location (Ligging) — direct bonus/penalty
    BigDecimal locationPoints = request.locationBonus().orElse(ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "LOCATION",
            "Location",
            "Ligging",
            locationPoints,
            locationPoints.compareTo(ZERO) != 0
                ? "Location adjustment: " + locationPoints + " pts"
                : "No adjustment"));

    // 11. Renovation (Renovatie) — version-specific
    BigDecimal renovationPoints = calculateRenovationPoints(request, config);
    breakdown.add(
        new WwsCategoryBreakdown(
            "RENOVATION",
            "Renovation",
            "Renovatie",
            renovationPoints,
            buildRenovationExplanation(request, config, renovationPoints)));

    // 12. Accessibility (Toegankelijkheid) — simplified: 3 pts per feature
    BigDecimal accessibilityPoints =
        request
            .accessibilityFeatures()
            .filter(f -> f > 0)
            .map(f -> new BigDecimal(f * 3))
            .orElse(ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "ACCESSIBILITY",
            "Accessibility",
            "Toegankelijkheid",
            accessibilityPoints,
            request
                .accessibilityFeatures()
                .filter(f -> f > 0)
                .map(f -> f + " features × 3 = " + accessibilityPoints + " pts")
                .orElse("Not provided")));

    // 13. Common Areas (Gemeenschappelijke ruimten) — 0.75 pt/m²
    BigDecimal commonAreaPoints =
        request
            .commonAreaSqm()
            .filter(sqm -> sqm.compareTo(ZERO) > 0)
            .map(sqm -> sqm.multiply(new BigDecimal("0.75")).setScale(2, HALF_UP))
            .orElse(ZERO);
    breakdown.add(
        new WwsCategoryBreakdown(
            "COMMON_AREAS",
            "Common Areas",
            "Gemeenschappelijke ruimten",
            commonAreaPoints,
            request
                .commonAreaSqm()
                .filter(sqm -> sqm.compareTo(ZERO) > 0)
                .map(sqm -> sqm + " m² × 0.75 = " + commonAreaPoints + " pts")
                .orElse("Not provided")));

    // Apply WOZ 33% cap (post-July 2024 only)
    if (config.hasWozCap() && rawWozPoints.compareTo(ZERO) > 0) {
      BigDecimal nonWozTotal =
          breakdown.stream()
              .filter(b -> !"WOZ_VALUE".equals(b.key()))
              .map(WwsCategoryBreakdown::points)
              .reduce(ZERO, BigDecimal::add);

      if (nonWozTotal.compareTo(ZERO) > 0) {
        BigDecimal maxWoz = nonWozTotal.multiply(WOZ_CAP_RATIO).setScale(2, HALF_UP);
        if (rawWozPoints.compareTo(maxWoz) > 0) {
          breakdown.set(
              wozIndex,
              new WwsCategoryBreakdown(
                  "WOZ_VALUE",
                  "WOZ Value",
                  "WOZ-waarde",
                  maxWoz,
                  wozExplanation + " → capped at 33% of non-WOZ points: " + maxWoz + " pts"));
        }
      }
    }

    return breakdown;
  }

  // WOZ: 2-component formula
  // Component I = WOZ / divider_I
  // Component II = (WOZ / surface_area_m2) / divider_II
  // Total = I + II, with minimum WOZ floor applied
  private static BigDecimal calculateWozPoints(
      WwsCalculationRequest request, WwsVersionConfig config) {
    return request
        .wozValue()
        .filter(woz -> woz.compareTo(ZERO) > 0)
        .map(
            woz -> {
              BigDecimal effectiveWoz = woz.max(config.wozMinimum());

              BigDecimal componentI = effectiveWoz.divide(config.wozDividerI(), 2, HALF_UP);

              BigDecimal componentII =
                  request
                      .surfaceAreaSqm()
                      .filter(area -> area.compareTo(ZERO) > 0)
                      .map(
                          area ->
                              effectiveWoz
                                  .divide(area, 2, HALF_UP)
                                  .divide(config.wozDividerII(), 2, HALF_UP))
                      .orElse(ZERO);

              return componentI.add(componentII).setScale(2, HALF_UP);
            })
        .orElse(ZERO);
  }

  private static String buildWozExplanation(
      WwsCalculationRequest request, WwsVersionConfig config, BigDecimal totalWozPoints) {
    if (request.wozValue().filter(woz -> woz.compareTo(ZERO) > 0).isEmpty()) {
      return "Not provided";
    }

    BigDecimal woz = request.wozValue().orElseThrow();
    BigDecimal effectiveWoz = woz.max(config.wozMinimum());
    BigDecimal compI = effectiveWoz.divide(config.wozDividerI(), 2, HALF_UP);

    StringBuilder sb = new StringBuilder();
    if (woz.compareTo(config.wozMinimum()) < 0) {
      sb.append("Min floor applied: €")
          .append(config.wozMinimum().setScale(0, HALF_UP))
          .append(". ");
    }
    sb.append("I: €")
        .append(effectiveWoz.setScale(0, HALF_UP))
        .append(" / €")
        .append(config.wozDividerI())
        .append(" = ")
        .append(compI)
        .append(" pts");

    request
        .surfaceAreaSqm()
        .filter(area -> area.compareTo(ZERO) > 0)
        .ifPresent(
            area -> {
              BigDecimal compII =
                  effectiveWoz.divide(area, 2, HALF_UP).divide(config.wozDividerII(), 2, HALF_UP);
              sb.append(" + II: (€")
                  .append(effectiveWoz.setScale(0, HALF_UP))
                  .append(" / ")
                  .append(area)
                  .append(" m²) / €")
                  .append(config.wozDividerII())
                  .append(" = ")
                  .append(compII)
                  .append(" pts");
            });

    sb.append(" = ").append(totalWozPoints).append(" pts total");
    return sb.toString();
  }

  // Outdoor space: old formula (2023) vs new formula (2024+)
  // 2023: floor(sqm / 25) × 2
  // 2024+: 2 + (sqm × 0.35), max 15. No outdoor space = -5 pts.
  private static BigDecimal calculateOutdoorPoints(
      WwsCalculationRequest request, WwsVersionConfig config) {
    if (config.newOutdoorFormula()) {
      return request
          .outdoorSpaceSqm()
          .filter(sqm -> sqm.compareTo(ZERO) > 0)
          .map(
              sqm -> {
                BigDecimal points =
                    new BigDecimal("2")
                        .add(sqm.multiply(new BigDecimal("0.35")))
                        .setScale(2, HALF_UP);
                return points.min(new BigDecimal("15"));
              })
          .orElse(new BigDecimal("-5"));
    } else {
      return request
          .outdoorSpaceSqm()
          .filter(sqm -> sqm.compareTo(ZERO) > 0)
          .map(sqm -> sqm.divide(new BigDecimal("25"), 0, DOWN).multiply(new BigDecimal("2")))
          .orElse(ZERO);
    }
  }

  private static String buildOutdoorExplanation(
      WwsCalculationRequest request, WwsVersionConfig config, BigDecimal points) {
    boolean hasOutdoor =
        request.outdoorSpaceSqm().filter(sqm -> sqm.compareTo(ZERO) > 0).isPresent();

    if (config.newOutdoorFormula()) {
      if (hasOutdoor) {
        BigDecimal sqm = request.outdoorSpaceSqm().orElseThrow();
        String base = "2 + (" + sqm + " m² × 0.35) = " + points + " pts";
        if (points.compareTo(new BigDecimal("15")) == 0) {
          return base + " (capped at 15)";
        }
        return base;
      } else {
        return "No outdoor space: -5 pts";
      }
    } else {
      if (hasOutdoor) {
        BigDecimal sqm = request.outdoorSpaceSqm().orElseThrow();
        return sqm + " m² / 25 × 2 = " + points + " pts";
      } else {
        return "Not provided";
      }
    }
  }

  // Renovation: 2023 = N/A, 2024+ = investment / €332 per point
  private static BigDecimal calculateRenovationPoints(
      WwsCalculationRequest request, WwsVersionConfig config) {
    if (!config.hasRenovation()) {
      return ZERO;
    }
    return request
        .renovationInvestment()
        .filter(inv -> inv.compareTo(ZERO) > 0)
        .map(inv -> inv.divide(RENOVATION_DIVISOR, 2, HALF_UP))
        .orElse(ZERO);
  }

  private static String buildRenovationExplanation(
      WwsCalculationRequest request, WwsVersionConfig config, BigDecimal points) {
    if (!config.hasRenovation()) {
      return "Not applicable for this system version";
    }
    return request
        .renovationInvestment()
        .filter(inv -> inv.compareTo(ZERO) > 0)
        .map(
            inv ->
                "€"
                    + inv.setScale(0, HALF_UP)
                    + " / €"
                    + RENOVATION_DIVISOR
                    + " = "
                    + points
                    + " pts")
        .orElse("Not provided");
  }

  // --- Classification & max rent ---

  // 2023: ≤141 = REGULATED, >141 = FREE_SECTOR
  // 2024+: ≤143 = REGULATED, 144-186 = MID_SEGMENT, ≥187 = FREE_SECTOR
  private static String classifyPoints(BigDecimal totalPoints, WwsVersionConfig config) {
    int points = totalPoints.setScale(0, HALF_UP).intValue();

    if (points <= config.regulatedMaxPoints()) {
      return "REGULATED";
    }
    if (config.hasMidSegment() && points <= config.midSegmentMaxPoints()) {
      return "MID_SEGMENT";
    }
    return "FREE_SECTOR";
  }

  // Max rent derived from threshold rent / threshold points ratio
  private static Optional<BigDecimal> calculateMaxRent(
      BigDecimal totalPoints, String classification, WwsVersionConfig config) {
    if ("FREE_SECTOR".equals(classification)) {
      return Optional.empty();
    }

    BigDecimal rate;
    if ("MID_SEGMENT".equals(classification) && config.hasMidSegment()) {
      rate =
          config
              .midSegmentThresholdRent()
              .divide(new BigDecimal(config.midSegmentMaxPoints()), 4, HALF_UP);
    } else {
      rate =
          config
              .socialThresholdRent()
              .divide(new BigDecimal(config.regulatedMaxPoints()), 4, HALF_UP);
    }

    return Optional.of(totalPoints.multiply(rate).setScale(2, HALF_UP));
  }

  // --- Helpers ---

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void deleteCalculation(WwsCalculationIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.getTeamId().orElseThrow();
    UUID userId = principal.getUserId();

    boolean deleted = wwsCalculationRepository.softDelete(identifier, teamId, userId);
    if (!deleted) {
      throw new NotFoundException("WWS calculation not found: " + identifier);
    }
  }

  private WwsCalculationResponse toResponse(WwsCalculation calc) {
    Optional<WwsCalculationRequest> inputData = Optional.empty();
    try {
      inputData =
          Optional.of(objectMapper.readValue(calc.getInputDataJson(), WwsCalculationRequest.class));
    } catch (Exception e) {
      log.warn("Failed to deserialize WWS input data for {}", calc.getIdentifier(), e);
    }

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
            .toList(),
        inputData);
  }

  private UUID resolveContractId(String contractIdentifier, UUID teamId) {
    return contractRepository
        .findByIdentifierAndTeamId(Sid.of(contractIdentifier), teamId)
        .map(c -> c.getId())
        .orElseThrow(() -> new NotFoundException("Contract not found: " + contractIdentifier));
  }
}
