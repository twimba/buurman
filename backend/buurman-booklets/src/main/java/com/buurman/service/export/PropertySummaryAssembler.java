package com.buurman.service.export;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.identifier.PropertyIdentifier;

/**
 * Loads a property + its active tenancy and residential details, and projects them into the
 * variables for {@code property-summary/generic.html}. KPI values are locale-neutral (area, bed/
 * bath, energy grade, year); money/dates are localized via {@link BookletFormatter}.
 */
@Component
public class PropertySummaryAssembler {

  // TODO(BUUR-106 Task 12): status, area, energy label/expiry and bed/bath (residential details)
  // all moved from `properties` to `units`/`unit_residential_details` in V068. This card's whole
  // purpose is displaying those fields, so it cannot be honestly assembled without a unit join.
  // No collaborators are wired here — reinstate PropertyRepository/ContractRepository/
  // BookletFormatter/EnumLabelResolver/QrCodeGenerator/Clock/appBaseUrl (and the unit-level
  // repositories this needs) alongside the real implementation.
  public Map<String, Object> assemble(PropertyIdentifier identifier, UUID teamId, Locale locale) {
    throw new UnsupportedOperationException(
        "Requires unit-level status/area/energy/residential-details join — added in Task 12"
            + " (BUUR-106)");
  }
}
