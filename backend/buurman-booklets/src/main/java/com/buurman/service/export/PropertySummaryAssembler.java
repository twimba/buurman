package com.buurman.service.export;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitResidentialDetails;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.util.MoneyAmount;

/**
 * Loads a property + its unit(s) + active tenancy/tenancies, and projects them into the variables
 * for {@code property-summary/generic.html}, a one-page A4-landscape card. KPI values are
 * locale-neutral (area, bed/bath, energy grade, year); money/dates are localized via {@link
 * BookletFormatter}.
 *
 * <p>Status/area/energy/bed-bath moved from {@code properties} to {@code units} in V070, so a
 * property with several units has N values for each of these, not one. The overwhelming majority of
 * properties still have exactly one unit (the implicit unit backfilled for every pre-existing
 * property), and for that case every field below reads exactly as it did before the migration —
 * sourced from that one unit instead of the property. A genuinely multi-unit property instead gets
 * an aggregate: an occupancy fraction for status, a sum for area, a sum for bed/bath counts, and a
 * summed headline rent across every unit's active contract. Energy label has no meaningful
 * aggregate (a letter grade can't be summed or averaged), so it renders as "—" unless every unit
 * agrees; the same is true of the per-tenancy "tenure" line, which is omitted entirely for a
 * multi-unit property since there is no single tenancy to show. A one-page card has no room for a
 * per-unit breakdown table — that lives in the full property dossier ({@link
 * PropertyBookletExporter}) instead.
 */
@Component
public class PropertySummaryAssembler {

  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final UnitResidentialDetailsRepository residentialDetailsRepository;
  private final ContractRepository contractRepository;
  private final BookletFormatter formatter;
  private final EnumLabelResolver enumLabels;
  private final QrCodeGenerator qrCodeGenerator;
  private final Clock clock;
  private final String appBaseUrl;

  public PropertySummaryAssembler(
      PropertyRepository propertyRepository,
      UnitRepository unitRepository,
      UnitResidentialDetailsRepository residentialDetailsRepository,
      ContractRepository contractRepository,
      BookletFormatter formatter,
      EnumLabelResolver enumLabels,
      QrCodeGenerator qrCodeGenerator,
      Clock clock,
      @Value("${booklet.app-base-url:https://app.buurman.io}") String appBaseUrl) {
    this.propertyRepository = propertyRepository;
    this.unitRepository = unitRepository;
    this.residentialDetailsRepository = residentialDetailsRepository;
    this.contractRepository = contractRepository;
    this.formatter = formatter;
    this.enumLabels = enumLabels;
    this.qrCodeGenerator = qrCodeGenerator;
    this.clock = clock;
    this.appBaseUrl = appBaseUrl;
  }

  public Map<String, Object> assemble(PropertyIdentifier identifier, UUID teamId, Locale locale) {
    Property property = propertyRepository.getByIdentifierAndTeamId(identifier, teamId);
    UUID propertyId = property.getId();
    List<Unit> units = unitRepository.findAllByPropertyIdAndTeamId(propertyId, teamId);

    Map<String, Object> v = new HashMap<>();
    v.put("lang", locale.getLanguage());
    v.put("dir", "ltr");
    v.put("propertyAddress", property.getStreet());
    v.put("propertyIdentifier", identifier.value());
    v.put("propertyTypeLabel", enumLabels.label(property.getPropertyType(), locale));
    v.put("propertyCategoryLabel", enumLabels.label(property.getPropertyCategory(), locale));
    v.put("yearBuilt", property.getYearBuilt().map(String::valueOf).orElse("—"));

    if (units.size() == 1) {
      assembleSingleUnit(v, units.get(0), teamId, locale);
    } else {
      assembleMultiUnit(v, units, teamId, locale);
    }

    v.put("parking", parking(property));
    v.put("renovated", property.getYearLastRenovated().map(String::valueOf).orElse(null));
    v.put(
        "qrDataUri",
        qrCodeGenerator.toSvgDataUri(appBaseUrl + "/properties/" + identifier.value()));
    v.put("generatedDate", formatter.date(LocalDate.now(clock), locale));
    return v;
  }

  /**
   * The common case: one unit, reading exactly as it did before dwelling fields moved off Property.
   */
  private void assembleSingleUnit(Map<String, Object> v, Unit unit, UUID teamId, Locale locale) {
    Optional<UnitResidentialDetails> residential =
        residentialDetailsRepository.findByUnitIdAndTeamId(unit.getId(), teamId);
    Optional<Contract> activeContract = contractRepository.findActiveByUnitId(unit.getId(), teamId);

    UnitStatus status = unit.getStatus();
    String statusLabel = enumLabels.label(status, locale);
    String heroWord = statusLabel.toUpperCase(locale);

    v.put("statusCode", status.name());
    v.put("statusLabel", statusLabel);
    v.put("heroStatusWord", heroWord);
    v.put("heroGroundClass", groundClass(status));
    v.put("heroSizeClass", formatter.heroSize(heroWord.length()));

    activeContract.ifPresent(
        c -> {
          LocalDate start = c.getStartDate();
          String end = c.getEndDate().map(d -> formatter.date(d, locale)).orElse("—");
          v.put("tenure", formatter.date(start, locale) + "  →  " + end);
        });

    v.put("area", unit.getAreaValue().map(a -> formatter.numberOrDash(a, locale)).orElse("—"));
    v.put("areaUnit", unit.getAreaUnit().orElse(""));
    v.put("bedBath", bedBath(residential));
    v.put("energyLabel", unit.getEnergyEfficiencyRating().orElse("—"));

    boolean vacant = activeContract.isEmpty();
    v.put("isVacant", vacant);
    v.put(
        "headlineMoney",
        activeContract.map(c -> formatter.money(c.getRentAmount(), locale)).orElse("—"));
    v.put(
        "energyExpiry",
        unit.getEnergyCertificateExpiryDate().map(d -> formatter.date(d, locale)).orElse(null));
  }

  /**
   * Several units: no single status/area/energy/bed-bath/tenancy exists, so every KPI becomes an
   * aggregate across the property's units (see the class doc for the rationale of each one).
   */
  private void assembleMultiUnit(
      Map<String, Object> v, List<Unit> units, UUID teamId, Locale locale) {
    int total = units.size();
    long occupied =
        units.stream()
            .filter(
                u ->
                    u.getStatus() == UnitStatus.OCCUPIED
                        || u.getStatus() == UnitStatus.SELF_OCCUPIED)
            .count();
    boolean allOccupied = total > 0 && occupied == total;
    boolean noneOccupied = occupied == 0;
    String statusCode = allOccupied ? "OCCUPIED" : noneOccupied ? "VACANT" : "MIXED";
    String occupiedWord = enumLabels.label(UnitStatus.OCCUPIED, locale);
    String fraction = occupied + "/" + total + " " + occupiedWord;

    v.put("statusCode", statusCode);
    v.put("statusLabel", fraction);
    String heroWord = fraction.toUpperCase(locale);
    v.put("heroStatusWord", heroWord);
    v.put("heroGroundClass", groundClassForCode(statusCode));
    v.put("heroSizeClass", formatter.heroSize(heroWord.length()));

    BigDecimal totalArea = BigDecimal.ZERO;
    boolean anyArea = false;
    for (Unit u : units) {
      if (u.getAreaValue().isPresent()) {
        totalArea = totalArea.add(u.getAreaValue().get());
        anyArea = true;
      }
    }
    v.put("area", anyArea ? formatter.numberOrDash(totalArea, locale) : "—");
    v.put("areaUnit", "sqm");

    int bedrooms = 0;
    int bathrooms = 0;
    boolean anyResidential = false;
    List<Contract> activeContracts = new ArrayList<>();
    for (Unit u : units) {
      Optional<UnitResidentialDetails> residential =
          residentialDetailsRepository.findByUnitIdAndTeamId(u.getId(), teamId);
      if (residential.isPresent()) {
        anyResidential = true;
        bedrooms += residential.get().getBedrooms().orElse(0);
        bathrooms += residential.get().getBathrooms().orElse(0);
      }
      contractRepository.findActiveByUnitId(u.getId(), teamId).ifPresent(activeContracts::add);
    }
    v.put("bedBath", anyResidential ? bedrooms + " / " + bathrooms : "—");

    // A letter grade cannot be summed or averaged: show it only when every unit agrees.
    List<String> ratings =
        units.stream()
            .map(Unit::getEnergyEfficiencyRating)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .toList();
    v.put("energyLabel", ratings.size() == 1 ? ratings.get(0) : "—");

    v.put("isVacant", activeContracts.isEmpty());
    v.put("headlineMoney", totalRent(activeContracts, locale));
    // No single tenancy exists across several units, so the tenure line is left unset — the
    // template only renders it when present.
    v.put("energyExpiry", null);
  }

  private String totalRent(List<Contract> activeContracts, Locale locale) {
    if (activeContracts.isEmpty()) {
      return "—";
    }
    String currency = activeContracts.get(0).getRentAmount().currency();
    BigDecimal sum = BigDecimal.ZERO;
    for (Contract c : activeContracts) {
      sum = sum.add(c.getRentAmount().value());
    }
    return formatter.money(MoneyAmount.of(sum, currency), locale);
  }

  private static String bedBath(Optional<UnitResidentialDetails> residential) {
    return residential
        .map(
            r ->
                r.getBedrooms().map(String::valueOf).orElse("—")
                    + " / "
                    + r.getBathrooms().map(String::valueOf).orElse("—"))
        .orElse("—");
  }

  private static @Nullable String parking(Property property) {
    List<String> parts = new ArrayList<>();
    property.getParkingSpaces().ifPresent(s -> parts.add(String.valueOf(s)));
    property.getParkingType().ifPresent(parts::add);
    return parts.isEmpty() ? null : String.join(" · ", parts);
  }

  private static String groundClass(UnitStatus status) {
    return switch (status) {
      case OCCUPIED, SELF_OCCUPIED -> "hs-occupied";
      case VACANT, LISTED -> "hs-vacant";
      default -> "hs-other";
    };
  }

  private static String groundClassForCode(String statusCode) {
    return switch (statusCode) {
      case "OCCUPIED" -> "hs-occupied";
      case "VACANT" -> "hs-vacant";
      default -> "hs-other";
    };
  }
}
