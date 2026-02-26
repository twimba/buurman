package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.PropertyFinancing;
import com.buurman.jooq.generated.tables.records.PropertyFinancingsRecord;
import com.buurman.util.CurrencyUtils;

@Component
public class PropertyFinancingRecordMapper {

  public Optional<PropertyFinancing> toDomain(@Nullable PropertyFinancingsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    PropertyFinancing financing = new PropertyFinancing();
    financing.setId(record.getId());
    financing.setIdentifier(record.getIdentifier());
    financing.setPropertyId(record.getPropertyId());
    financing.setTeamId(record.getTeamId());
    financing.setFinancingType(PropertyFinancing.FinancingType.valueOf(record.getFinancingType()));
    financing.setRateType(PropertyFinancing.RateType.valueOf(record.getRateType()));
    financing.setLenderName(Optional.ofNullable(record.getLenderName()));
    financing.setLoanNumber(Optional.ofNullable(record.getLoanNumber()));
    financing.setOriginalAmount(
        CurrencyUtils.toMajorUnits(record.getOriginalAmount(), record.getOriginalAmountCurrency()));
    financing.setOriginalAmountCurrency(record.getOriginalAmountCurrency());
    financing.setCurrentBalance(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getCurrentBalance(), record.getCurrentBalanceCurrency())));
    financing.setCurrentBalanceCurrency(Optional.ofNullable(record.getCurrentBalanceCurrency()));
    financing.setInterestRate(Optional.ofNullable(record.getInterestRate()));
    financing.setMonthlyPayment(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getMonthlyPayment(), record.getMonthlyPaymentCurrency())));
    financing.setMonthlyPaymentCurrency(Optional.ofNullable(record.getMonthlyPaymentCurrency()));
    financing.setPaymentVariable(Boolean.TRUE.equals(record.getPaymentVariable()));
    financing.setStartDate(record.getStartDate());
    financing.setEndDate(Optional.ofNullable(record.getEndDate()));
    financing.setTermMonths(Optional.ofNullable(record.getTermMonths()));
    financing.setStatus(PropertyFinancing.FinancingStatus.valueOf(record.getStatus()));
    financing.setNotes(Optional.ofNullable(record.getNotes()));
    financing.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    financing.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    financing.setCreatedBy(record.getCreatedBy());
    financing.setUpdatedBy(record.getUpdatedBy());
    financing.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(financing);
  }
}
