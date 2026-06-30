package com.buurman.service.export;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
import com.buurman.domain.PropertyResidentialDetails;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyResidentialDetailsRepository;

/**
 * Loads a property + its active tenancy and residential details, and projects them into the
 * variables for {@code property-summary/generic.html}. KPI values are locale-neutral (area, bed/
 * bath, energy grade, year); money/dates are localized via {@link BookletFormatter}.
 */
@Component
public class PropertySummaryAssembler {

  private final PropertyRepository propertyRepository;
  private final PropertyResidentialDetailsRepository residentialDetailsRepository;
  private final ContractRepository contractRepository;
  private final BookletFormatter formatter;
  private final EnumLabelResolver enumLabels;
  private final QrCodeGenerator qrCodeGenerator;
  private final Clock clock;
  private final String appBaseUrl;

  public PropertySummaryAssembler(
      PropertyRepository propertyRepository,
      PropertyResidentialDetailsRepository residentialDetailsRepository,
      ContractRepository contractRepository,
      BookletFormatter formatter,
      EnumLabelResolver enumLabels,
      QrCodeGenerator qrCodeGenerator,
      Clock clock,
      @Value("${booklet.app-base-url:https://app.buurman.io}") String appBaseUrl) {
    this.propertyRepository = propertyRepository;
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
    Optional<PropertyResidentialDetails> residential =
        residentialDetailsRepository.findByPropertyIdAndTeamId(propertyId, teamId);
    Optional<Contract> activeContract =
        contractRepository.findByPropertyId(propertyId, teamId).stream()
            .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
            .max(Comparator.comparing(Contract::getStartDate));

    Property.PropertyStatus status = property.getStatus();
    String statusLabel = enumLabels.label(status, locale);
    String heroWord = statusLabel.toUpperCase(locale);

    Map<String, Object> v = new HashMap<>();
    v.put("lang", locale.getLanguage());
    v.put("dir", "ltr");
    v.put("propertyAddress", property.getStreet());
    v.put("propertyIdentifier", identifier.value());
    v.put("propertyTypeLabel", enumLabels.label(property.getPropertyType(), locale));
    v.put("propertyCategoryLabel", enumLabels.label(property.getPropertyCategory(), locale));
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

    v.put("area", property.getAreaValue().map(a -> formatter.numberOrDash(a, locale)).orElse("—"));
    v.put("areaUnit", property.getAreaUnit().orElse(""));
    v.put("bedBath", bedBath(residential));
    v.put("energyLabel", property.getEnergyEfficiencyRating().orElse("—"));
    v.put("yearBuilt", property.getYearBuilt().map(String::valueOf).orElse("—"));

    boolean vacant = activeContract.isEmpty();
    v.put("isVacant", vacant);
    v.put(
        "headlineMoney",
        activeContract.map(c -> formatter.money(c.getRentAmount(), locale)).orElse("—"));

    v.put("parking", parking(property));
    v.put(
        "energyExpiry",
        property.getEnergyCertificateExpiryDate().map(d -> formatter.date(d, locale)).orElse(null));
    v.put("renovated", property.getYearLastRenovated().map(String::valueOf).orElse(null));
    // Compound feature strings (safety/utilities/accessibility) would need localized bundles; they
    // are intentionally not rendered yet (the template has no tiles for them).

    v.put(
        "qrDataUri",
        qrCodeGenerator.toSvgDataUri(appBaseUrl + "/properties/" + identifier.value()));
    v.put("generatedDate", formatter.date(LocalDate.now(clock), locale));
    return v;
  }

  private static String bedBath(Optional<PropertyResidentialDetails> residential) {
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

  private static String groundClass(Property.PropertyStatus status) {
    return switch (status) {
      case OCCUPIED, SELF_OCCUPIED -> "hs-occupied";
      case VACANT, LISTED -> "hs-vacant";
      default -> "hs-other";
    };
  }
}
