package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.jooq.generated.tables.records.ContractsRecord;

@Component
public class ContractRecordMapper {

  public Contract toDomain(ContractsRecord record) {
    if (record == null) {
      return null;
    }

    Contract contract = new Contract();
    contract.setId(record.getId());
    contract.setIdentifier(record.getIdentifier());
    contract.setTeamId(record.getTeamId());
    contract.setPropertyId(record.getPropertyId());
    contract.setContractType(Contract.ContractType.valueOf(record.getContractType()));
    contract.setStartDate(record.getStartDate());
    contract.setEndDate(record.getEndDate());
    contract.setSignedDate(record.getSignedDate());
    contract.setRentAmount(record.getRentAmount());
    contract.setDepositAmount(record.getDepositAmount());
    contract.setSecurityDeposit(record.getSecurityDeposit());
    contract.setCurrency(record.getCurrency());
    contract.setPaymentFrequency(Contract.PaymentFrequency.valueOf(record.getPaymentFrequency()));
    contract.setPaymentDueDay(record.getPaymentDueDay());
    contract.setAutoRenewal(record.getAutoRenewal());
    contract.setRenewalNoticeDays(record.getRenewalNoticeDays());
    contract.setTerminationNoticeDays(record.getTerminationNoticeDays());
    contract.setLateFeePercentage(record.getLateFeePercentage());
    contract.setStatus(Contract.ContractStatus.valueOf(record.getStatus()));
    contract.setTermsAndConditions(record.getTermsAndConditions());
    contract.setNotes(record.getNotes());
    contract.setCreatedAt(
        record.getCreatedAt() != null ? record.getCreatedAt().toInstant(UTC) : null);
    contract.setUpdatedAt(
        record.getUpdatedAt() != null ? record.getUpdatedAt().toInstant(UTC) : null);
    contract.setCreatedBy(record.getCreatedBy());
    contract.setUpdatedBy(record.getUpdatedBy());
    contract.setDeletedAt(
        record.getDeletedAt() != null ? record.getDeletedAt().toInstant(UTC) : null);

    return contract;
  }
}
