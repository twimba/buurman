package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.domain.ContractExtension;
import com.buurman.jooq.generated.tables.records.ContractExtensionsRecord;
import com.buurman.util.MoneyAmount;

@Component
public class ContractExtensionRecordMapper {

  public ContractExtension toDomain(ContractExtensionsRecord record) {
    ContractExtension ext = new ContractExtension();
    ext.setId(record.getId());
    ext.setIdentifier(Optional.of(record.getIdentifier()));
    ext.setTeamId(record.getTeamId());
    ext.setContractId(record.getContractId());
    ext.setExtensionNumber(record.getExtensionNumber());
    ext.setPreviousEndDate(record.getPreviousEndDate());
    ext.setNewEndDate(Optional.ofNullable(record.getNewEndDate()));
    ext.setPreviousRentAmount(
        MoneyAmount.ofMinorUnits(record.getPreviousRentAmount(), record.getPreviousRentCurrency()));
    ext.setNewRentAmount(
        MoneyAmount.ofMinorUnits(record.getNewRentAmount(), record.getNewRentCurrency()));
    ext.setRentAdjustmentType(
        ContractExtension.RentAdjustmentType.valueOf(record.getRentAdjustmentType()));
    ext.setRentAdjustmentValue(Optional.ofNullable(record.getRentAdjustmentValue()));
    ext.setStatus(ContractExtension.ExtensionStatus.valueOf(record.getStatus()));
    ext.setTriggerType(ContractExtension.TriggerType.valueOf(record.getTriggerType()));
    ext.setRentPeriodId(Optional.ofNullable(record.getRentPeriodId()));
    ext.setNotes(Optional.ofNullable(record.getNotes()));
    ext.setDeclinedReason(Optional.ofNullable(record.getDeclinedReason()));
    ext.setActivatedAt(
        Optional.ofNullable(record.getActivatedAt()).map(dt -> dt.toInstant(UTC)));
    ext.setActivatedBy(Optional.ofNullable(record.getActivatedBy()));
    ext.setConfirmedAt(
        Optional.ofNullable(record.getConfirmedAt()).map(dt -> dt.toInstant(UTC)));
    ext.setConfirmedBy(Optional.ofNullable(record.getConfirmedBy()));
    ext.setSupersededAt(
        Optional.ofNullable(record.getSupersededAt()).map(dt -> dt.toInstant(UTC)));
    ext.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    ext.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    ext.setCreatedBy(record.getCreatedBy());
    ext.setUpdatedBy(record.getUpdatedBy());
    ext.setDeletedAt(
        Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));
    return ext;
  }
}
