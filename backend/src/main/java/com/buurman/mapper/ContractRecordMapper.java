package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.jooq.generated.tables.records.ContractsRecord;
import com.buurman.util.CurrencyUtils;

@Component
public class ContractRecordMapper {

  public @Nullable Contract toDomain(@Nullable ContractsRecord record) {
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
    contract.setRentAmount(
        CurrencyUtils.toMajorUnits(record.getRentAmount(), record.getRentAmountCurrency()));
    contract.setRentAmountCurrency(record.getRentAmountCurrency());
    Long depositAmount = record.getDepositAmount();
    String depositCurrency = record.getDepositAmountCurrency();
    if (depositAmount != null && depositCurrency != null) {
      contract.setDepositAmount(CurrencyUtils.toMajorUnits(depositAmount, depositCurrency));
    }
    contract.setDepositAmountCurrency(depositCurrency);
    Long securityDeposit = record.getSecurityDeposit();
    String securityDepositCurrency = record.getSecurityDepositCurrency();
    if (securityDeposit != null && securityDepositCurrency != null) {
      contract.setSecurityDeposit(
          CurrencyUtils.toMajorUnits(securityDeposit, securityDepositCurrency));
    }
    contract.setSecurityDepositCurrency(record.getSecurityDepositCurrency());
    contract.setPaymentFrequency(Contract.PaymentFrequency.valueOf(record.getPaymentFrequency()));
    contract.setPaymentDueDay(record.getPaymentDueDay());
    contract.setAutoRenewal(record.getAutoRenewal());
    contract.setRenewalNoticeDays(record.getRenewalNoticeDays());
    contract.setTerminationNoticeDays(record.getTerminationNoticeDays());
    contract.setLateFeePercentage(record.getLateFeePercentage());
    contract.setStatus(Contract.ContractStatus.valueOf(record.getStatus()));
    contract.setTermsAndConditions(record.getTermsAndConditions());
    contract.setNotes(record.getNotes());
    if (record.getCreatedAt() != null) {
      contract.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    }
    if (record.getUpdatedAt() != null) {
      contract.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    }
    contract.setCreatedBy(record.getCreatedBy());
    contract.setUpdatedBy(record.getUpdatedBy());
    contract.setDeletedAt(
        record.getDeletedAt() != null ? record.getDeletedAt().toInstant(UTC) : null);

    return contract;
  }
}
