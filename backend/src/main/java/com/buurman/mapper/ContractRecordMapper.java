package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.jooq.generated.tables.records.ContractsRecord;
import com.buurman.util.CurrencyUtils;

@Component
public class ContractRecordMapper {

  public Optional<Contract> toDomain(@Nullable ContractsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    Contract contract = new Contract();
    contract.setId(record.getId());
    contract.setIdentifier(record.getIdentifier());
    contract.setTeamId(record.getTeamId());
    contract.setPropertyId(record.getPropertyId());
    contract.setContractType(Contract.ContractType.valueOf(record.getContractType()));
    contract.setStartDate(record.getStartDate());
    contract.setEndDate(Optional.ofNullable(record.getEndDate()));
    contract.setSignedDate(Optional.ofNullable(record.getSignedDate()));
    contract.setRentAmount(
        CurrencyUtils.toMajorUnits(record.getRentAmount(), record.getRentAmountCurrency()));
    contract.setRentAmountCurrency(record.getRentAmountCurrency());
    Long depositAmount = record.getDepositAmount();
    String depositCurrency = record.getDepositAmountCurrency();
    if (depositAmount != null && depositCurrency != null) {
      contract.setDepositAmount(
          Optional.of(CurrencyUtils.toMajorUnits(depositAmount, depositCurrency)));
    }
    contract.setDepositAmountCurrency(Optional.ofNullable(depositCurrency));
    Long securityDeposit = record.getSecurityDeposit();
    String securityDepositCurrency = record.getSecurityDepositCurrency();
    if (securityDeposit != null && securityDepositCurrency != null) {
      contract.setSecurityDeposit(
          Optional.of(CurrencyUtils.toMajorUnits(securityDeposit, securityDepositCurrency)));
    }
    contract.setSecurityDepositCurrency(Optional.ofNullable(record.getSecurityDepositCurrency()));
    contract.setPaymentFrequency(Contract.PaymentFrequency.valueOf(record.getPaymentFrequency()));
    contract.setPaymentDueDay(Optional.ofNullable(record.getPaymentDueDay()));
    contract.setAutoRenewal(record.getAutoRenewal() != null ? record.getAutoRenewal() : false);
    contract.setRenewalNoticeDays(
        record.getRenewalNoticeDays() != null ? record.getRenewalNoticeDays() : 30);
    contract.setTerminationNoticeDays(
        record.getTerminationNoticeDays() != null ? record.getTerminationNoticeDays() : 30);
    contract.setLateFeePercentage(Optional.ofNullable(record.getLateFeePercentage()));
    contract.setStatus(Contract.ContractStatus.valueOf(record.getStatus()));
    contract.setTermsAndConditions(Optional.ofNullable(record.getTermsAndConditions()));
    contract.setNotes(Optional.ofNullable(record.getNotes()));
    contract.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    contract.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    contract.setCreatedBy(record.getCreatedBy());
    contract.setUpdatedBy(record.getUpdatedBy());
    contract.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(contract);
  }
}
