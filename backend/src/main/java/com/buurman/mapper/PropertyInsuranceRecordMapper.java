package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.PropertyInsurance;
import com.buurman.jooq.generated.tables.records.PropertyInsurancesRecord;
import com.buurman.util.MoneyAmount;

@Component
public class PropertyInsuranceRecordMapper {

  public Optional<PropertyInsurance> toDomain(@Nullable PropertyInsurancesRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    PropertyInsurance insurance = new PropertyInsurance();
    insurance.setId(record.getId());
    insurance.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    insurance.setPropertyId(record.getPropertyId());
    insurance.setTeamId(record.getTeamId());
    insurance.setInsuranceType(PropertyInsurance.InsuranceType.valueOf(record.getInsuranceType()));
    insurance.setProvider(Optional.ofNullable(record.getProvider()));
    insurance.setPolicyNumber(Optional.ofNullable(record.getPolicyNumber()));
    insurance.setCoverageAmount(
        MoneyAmount.ofNullable(record.getCoverageAmount(), record.getCoverageAmountCurrency()));
    insurance.setAnnualPremium(
        MoneyAmount.of(record.getAnnualPremium(), record.getAnnualPremiumCurrency()));
    insurance.setPaymentFrequency(record.getPaymentFrequency());
    insurance.setStartDate(Optional.ofNullable(record.getStartDate()));
    insurance.setEndDate(Optional.ofNullable(record.getEndDate()));
    insurance.setStatus(PropertyInsurance.InsuranceStatus.valueOf(record.getStatus()));
    insurance.setNotes(Optional.ofNullable(record.getNotes()));
    insurance.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    insurance.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    insurance.setCreatedBy(record.getCreatedBy());
    insurance.setUpdatedBy(record.getUpdatedBy());
    insurance.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(insurance);
  }
}
