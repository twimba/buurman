package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.Contract;
import com.buurman.domain.metadata.ContractCountryMetadata;
import com.buurman.domain.metadata.CountryMetadataSerializer;
import com.buurman.jooq.generated.tables.records.ContractsRecord;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContractRecordMapper {

  private final CountryMetadataSerializer countryMetadataSerializer;

  public Optional<Contract> toDomain(@Nullable ContractsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    Contract contract = new Contract();
    contract.setId(record.getId());
    contract.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    contract.setTeamId(record.getTeamId());
    contract.setPropertyId(record.getPropertyId());
    contract.setContractType(Contract.ContractType.valueOf(record.getContractType()));
    contract.setStartDate(record.getStartDate());
    contract.setEndDate(Optional.ofNullable(record.getEndDate()));
    contract.setSignedDate(Optional.ofNullable(record.getSignedDate()));
    contract.setRentAmount(MoneyAmount.of(record.getRentAmount(), record.getRentAmountCurrency()));
    contract.setDepositAmount(
        MoneyAmount.ofNullable(record.getDepositAmount(), record.getDepositAmountCurrency()));
    contract.setSecurityDeposit(
        MoneyAmount.ofNullable(record.getSecurityDeposit(), record.getSecurityDepositCurrency()));
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

    // Renewal configuration columns (added in V034)
    contract.setRenewalMode(Contract.RenewalMode.valueOf(record.getRenewalMode()));
    contract.setRenewalTermMonths(Optional.ofNullable(record.getRenewalTermMonths()));
    contract.setMaxRenewals(Optional.ofNullable(record.getMaxRenewals()));
    contract.setLandlordNoticeDays(record.getLandlordNoticeDays());
    contract.setTenantNoticeDays(record.getTenantNoticeDays());
    contract.setRequiresTenantConfirmation(record.getRequiresTenantConfirmation());
    contract.setRentAdjustmentType(
        com.buurman.domain.ContractExtension.RentAdjustmentType.valueOf(
            record.getRentAdjustmentType()));
    contract.setRentAdjustmentValue(Optional.ofNullable(record.getRentAdjustmentValue()));
    contract.setLandlordType(
        Optional.ofNullable(record.getLandlordType()).map(Contract.LandlordType::valueOf));
    contract.setRegionCode(Optional.ofNullable(record.getRegionCode()));

    // country_code and country_metadata columns (added in V012)
    String countryCode = record.get(org.jooq.impl.DSL.field("country_code", String.class));
    contract.setCountryCode(Optional.of(countryCode));
    if (countryCode != null) {
      org.jooq.JSONB metadataJsonb =
          record.get(org.jooq.impl.DSL.field("country_metadata", org.jooq.JSONB.class));
      if (metadataJsonb != null) {
        ContractCountryMetadata metadata =
            countryMetadataSerializer.deserialize(metadataJsonb.data(), countryCode);
        contract.setCountryMetadata(Optional.ofNullable(metadata));
      }
    }

    return Optional.of(contract);
  }
}
