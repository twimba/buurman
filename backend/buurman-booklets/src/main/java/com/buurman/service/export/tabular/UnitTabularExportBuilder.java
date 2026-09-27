package com.buurman.service.export.tabular;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitResidentialDetails;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.UnitResidentialDetailsRepository;
import com.buurman.service.ContractPartyService;
import com.buurman.service.export.BookletFormatter;
import com.buurman.service.export.EnumLabelResolver;

import lombok.RequiredArgsConstructor;

/**
 * TabularExport for an explicit list of units (e.g. one property's units), localized via {@link
 * EnumLabelResolver}/{@link BookletFormatter} — unlike the team-wide property/expense exports,
 * which stay raw English. Bedrooms/bathrooms/furnished come from {@link UnitResidentialDetails}
 * (present only for APARTMENT units); tenant/monthly rent come from each unit's active contract.
 * Every per-unit lookup is scoped with that unit's own {@code teamId}, so callers only need to have
 * assembled the {@code units} list within the correct team in the first place.
 */
@Component
@RequiredArgsConstructor
public class UnitTabularExportBuilder {

  private final UnitResidentialDetailsRepository residentialDetailsRepository;
  private final ContractRepository contractRepository;
  private final ContractPartyService contractPartyService;
  private final EnumLabelResolver enumLabels;
  private final BookletFormatter formatter;

  public TabularExport build(List<Unit> units, Locale locale) {
    TabularExport export = new TabularExport("units");
    TabularSheet sheet =
        export.addSheet(
            "Units",
            List.of(
                TabularColumn.text("Unit Number"),
                TabularColumn.text("Name"),
                TabularColumn.of("Floor", TabularColumnFormat.INTEGER),
                TabularColumn.text("Type"),
                TabularColumn.text("Status"),
                TabularColumn.of("Area (sqm)", TabularColumnFormat.NUMBER),
                TabularColumn.text("Energy Label"),
                TabularColumn.of("Bedrooms", TabularColumnFormat.INTEGER),
                TabularColumn.of("Bathrooms", TabularColumnFormat.INTEGER),
                TabularColumn.text("Furnished"),
                TabularColumn.text("Cost Share"),
                TabularColumn.text("Tenant"),
                TabularColumn.of("Monthly Rent", TabularColumnFormat.CURRENCY)));

    for (Unit unit : units) {
      Optional<UnitResidentialDetails> residential =
          residentialDetailsRepository.findByUnitIdAndTeamId(unit.getId(), unit.getTeamId());
      Optional<Contract> activeContract =
          contractRepository.findActiveByUnitId(unit.getId(), unit.getTeamId());
      String tenant = activeContract.map(c -> tenantName(c, unit.getTeamId())).orElse("");
      String rent = activeContract.map(c -> formatter.money(c.getRentAmount(), locale)).orElse("");

      sheet.addRow(
          unit.getUnitNumber(),
          unit.getName().orElse(""),
          unit.getFloor().map(Object::toString).orElse(""),
          enumLabels.label(unit.getUnitType(), locale),
          enumLabels.label(unit.getStatus(), locale),
          unit.getAreaValue().isPresent() ? unit.getAreaValue().get() : "",
          unit.getEnergyEfficiencyRating().orElse(""),
          residential.flatMap(UnitResidentialDetails::getBedrooms).map(Object::toString).orElse(""),
          residential
              .flatMap(UnitResidentialDetails::getBathrooms)
              .map(Object::toString)
              .orElse(""),
          residential.map(r -> r.isFurnished() ? "Yes" : "No").orElse(""),
          unit.getAllocationShare().map(s -> s + "%").orElse(""),
          tenant,
          rent);
    }
    return export;
  }

  private String tenantName(Contract contract, UUID teamId) {
    Map<UUID, Contact> primaryContacts =
        contractPartyService.getPrimaryContactsForContracts(List.of(contract.getId()), teamId);
    Contact contact = primaryContacts.get(contract.getId());
    return contact != null ? contact.getDisplayName() : "";
  }
}
