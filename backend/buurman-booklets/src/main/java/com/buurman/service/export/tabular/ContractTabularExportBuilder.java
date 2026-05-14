package com.buurman.service.export.tabular;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.domain.Property;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;

import lombok.RequiredArgsConstructor;

/** Single-sheet TabularExport for all team contracts, joined to property address. */
@Component
@RequiredArgsConstructor
public class ContractTabularExportBuilder {

  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;

  public TabularExport build(UUID teamId) {
    List<Contract> contracts = contractRepository.findAllByTeamId(teamId);

    Map<UUID, String> propertyById = new HashMap<>();
    Map<UUID, String> propertyAddressById = new HashMap<>();
    for (Property p : propertyRepository.findAllByTeamId(teamId)) {
      propertyById.put(p.getId(), p.getIdentifier().map(Object::toString).orElse(""));
      propertyAddressById.put(p.getId(), p.getStreet() + ", " + p.getCity());
    }

    TabularExport export = new TabularExport("contracts");
    TabularSheet sheet =
        export.addSheet(
            "Contracts",
            List.of(
                TabularColumn.text("Identifier"),
                TabularColumn.text("Property"),
                TabularColumn.text("Property Address"),
                TabularColumn.text("Contract Type"),
                TabularColumn.text("Status"),
                TabularColumn.of("Start Date", TabularColumnFormat.DATE),
                TabularColumn.of("End Date", TabularColumnFormat.DATE),
                TabularColumn.of("Signed Date", TabularColumnFormat.DATE),
                TabularColumn.of("Rent Amount", TabularColumnFormat.CURRENCY),
                TabularColumn.text("Currency"),
                TabularColumn.text("Payment Frequency"),
                TabularColumn.of("Payment Due Day", TabularColumnFormat.INTEGER),
                TabularColumn.of("Deposit Amount", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Termination Notice (days)", TabularColumnFormat.INTEGER),
                TabularColumn.of("Landlord Notice (days)", TabularColumnFormat.INTEGER),
                TabularColumn.of("Tenant Notice (days)", TabularColumnFormat.INTEGER),
                TabularColumn.text("Renewal Mode"),
                TabularColumn.text("Country"),
                TabularColumn.text("Region"),
                TabularColumn.text("Created At"),
                TabularColumn.text("Updated At")));

    for (Contract c : contracts) {
      sheet.addRow(
          c.getIdentifier().map(Object::toString).orElse(""),
          propertyById.getOrDefault(c.getPropertyId(), ""),
          propertyAddressById.getOrDefault(c.getPropertyId(), ""),
          c.getContractType().name(),
          c.getStatus().name(),
          c.getStartDate() != null ? c.getStartDate().toString() : "",
          c.getEndDate().map(Object::toString).orElse(""),
          c.getSignedDate().map(Object::toString).orElse(""),
          c.getRentAmount() != null ? c.getRentAmount().value() : "",
          c.getRentAmount() != null ? c.getRentAmount().currency() : "",
          c.getPaymentFrequency().name(),
          c.getPaymentDueDay().isPresent() ? c.getPaymentDueDay().get() : "",
          c.getDepositAmount().isPresent() ? c.getDepositAmount().get().value() : "",
          c.getTerminationNoticeDays(),
          c.getLandlordNoticeDays(),
          c.getTenantNoticeDays(),
          c.getRenewalMode().name(),
          c.getCountryCode().orElse(""),
          c.getRegionCode().orElse(""),
          c.getCreatedAt() != null ? c.getCreatedAt().toString() : "",
          c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
    }
    return export;
  }
}
