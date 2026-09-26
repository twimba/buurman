package com.buurman.service.export;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;

/**
 * Loads a property + its active tenancy and residential details, and projects them into the
 * variables for {@code property-summary/generic.html}. KPI values are locale-neutral (area, bed/
 * bath, energy grade, year); money/dates are localized via {@link BookletFormatter}.
 */
@Component
public class PropertySummaryAssembler {

  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final BookletFormatter formatter;
  private final EnumLabelResolver enumLabels;
  private final QrCodeGenerator qrCodeGenerator;
  private final Clock clock;
  private final String appBaseUrl;

  public PropertySummaryAssembler(
      PropertyRepository propertyRepository,
      ContractRepository contractRepository,
      BookletFormatter formatter,
      EnumLabelResolver enumLabels,
      QrCodeGenerator qrCodeGenerator,
      Clock clock,
      @Value("${booklet.app-base-url:https://app.buurman.io}") String appBaseUrl) {
    this.propertyRepository = propertyRepository;
    this.contractRepository = contractRepository;
    this.formatter = formatter;
    this.enumLabels = enumLabels;
    this.qrCodeGenerator = qrCodeGenerator;
    this.clock = clock;
    this.appBaseUrl = appBaseUrl;
  }

  // TODO(BUUR-106 Task 12): status, area, energy label/expiry and bed/bath (residential details)
  // all moved from `properties` to `units`/`unit_residential_details` in V068. This card's whole
  // purpose is displaying those fields, so it cannot be honestly assembled without a unit join.
  public Map<String, Object> assemble(PropertyIdentifier identifier, UUID teamId, Locale locale) {
    throw new UnsupportedOperationException(
        "Requires unit-level status/area/energy/residential-details join — added in Task 12"
            + " (BUUR-106)");
  }
}
