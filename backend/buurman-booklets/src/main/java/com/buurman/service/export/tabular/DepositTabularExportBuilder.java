package com.buurman.service.export.tabular;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.Deposit;
import com.buurman.domain.DepositDeduction;
import com.buurman.domain.Property;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DepositRepository;
import com.buurman.repository.PropertyRepository;

import lombok.RequiredArgsConstructor;

/** Two-sheet TabularExport: one row per deposit, plus every deduction with its reason. */
@Component
@RequiredArgsConstructor
public class DepositTabularExportBuilder {

  private final DepositRepository depositRepository;
  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final ContactRepository contactRepository;

  public TabularExport build(UUID teamId) {
    List<Deposit> deposits = depositRepository.findAllByTeamId(teamId);
    Map<UUID, List<DepositDeduction>> deductionsByDeposit =
        depositRepository.findAllDeductionsByTeamId(teamId).stream()
            .collect(Collectors.groupingBy(DepositDeduction::getDepositId));

    Map<UUID, Contract> contractsById = new HashMap<>();
    for (Contract c : contractRepository.findAllByTeamId(teamId)) {
      contractsById.put(c.getId(), c);
    }
    Map<UUID, String> propertyAddressById = new HashMap<>();
    for (Property p : propertyRepository.findAllByTeamId(teamId)) {
      propertyAddressById.put(p.getId(), p.getStreet() + ", " + p.getCity());
    }
    Map<UUID, String> contactNameById = new HashMap<>();
    for (Contact c : contactRepository.findAllByTeamId(teamId)) {
      contactNameById.put(c.getId(), c.getDisplayName());
    }

    TabularExport export = new TabularExport("deposits");
    TabularSheet sheet =
        export.addSheet(
            "Deposits",
            List.of(
                TabularColumn.text("Identifier"),
                TabularColumn.text("Contract"),
                TabularColumn.text("Property"),
                TabularColumn.text("Tenant"),
                TabularColumn.text("Status"),
                TabularColumn.of("Amount", TabularColumnFormat.CURRENCY),
                TabularColumn.text("Currency"),
                TabularColumn.of("Received Date", TabularColumnFormat.DATE),
                TabularColumn.text("Held At"),
                TabularColumn.of("Return Due", TabularColumnFormat.DATE),
                TabularColumn.of("Deductions", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Returned", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Returned Date", TabularColumnFormat.DATE),
                TabularColumn.of("Refundable", TabularColumnFormat.CURRENCY),
                TabularColumn.text("Notes")));
    TabularSheet deductionsSheet =
        export.addSheet(
            "Deductions",
            List.of(
                TabularColumn.text("Deposit"),
                TabularColumn.text("Contract"),
                TabularColumn.text("Tenant"),
                TabularColumn.of("Date", TabularColumnFormat.DATE),
                TabularColumn.text("Reason"),
                TabularColumn.of("Amount", TabularColumnFormat.CURRENCY),
                TabularColumn.text("Currency")));

    for (Deposit d : deposits) {
      Contract contract = contractsById.get(d.getContractId());
      String contractIdentifier =
          contract == null ? "" : contract.getIdentifier().map(Object::toString).orElse("");
      String propertyAddress =
          contract == null ? "" : propertyAddressById.getOrDefault(contract.getPropertyId(), "");
      String tenant = d.getContactId().map(id -> contactNameById.getOrDefault(id, "")).orElse("");
      List<DepositDeduction> deductions = deductionsByDeposit.getOrDefault(d.getId(), List.of());
      BigDecimal deductionsTotal =
          deductions.stream()
              .map(x -> x.getAmount().value())
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal refundable =
          d.getAmount()
              .value()
              .subtract(deductionsTotal)
              .subtract(d.getReturnedAmount())
              .max(BigDecimal.ZERO);
      String depositIdentifier = d.getIdentifier().map(Object::toString).orElse("");

      sheet.addRow(
          depositIdentifier,
          contractIdentifier,
          propertyAddress,
          tenant,
          d.getStatus().name(),
          d.getAmount().value(),
          d.getAmount().currency(),
          d.getReceivedDate().map(Object::toString).orElse(""),
          d.getHeldWhere().orElse(""),
          d.getReturnDueDate().map(Object::toString).orElse(""),
          deductionsTotal,
          d.getReturnedAmount(),
          d.getReturnedDate().map(Object::toString).orElse(""),
          refundable,
          d.getNotes().orElse(""));

      for (DepositDeduction x : deductions) {
        deductionsSheet.addRow(
            depositIdentifier,
            contractIdentifier,
            tenant,
            x.getDeductionDate().toString(),
            x.getReason(),
            x.getAmount().value(),
            x.getAmount().currency());
      }
    }
    return export;
  }
}
