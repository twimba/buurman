package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.PropertyTax;
import com.buurman.jooq.generated.tables.records.PropertyTaxesRecord;
import com.buurman.util.MoneyAmount;

@Component
public class PropertyTaxRecordMapper {

  public Optional<PropertyTax> toDomain(@Nullable PropertyTaxesRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    PropertyTax tax = new PropertyTax();
    tax.setId(record.getId());
    tax.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    tax.setPropertyId(record.getPropertyId());
    tax.setTeamId(record.getTeamId());
    tax.setTaxType(PropertyTax.TaxType.valueOf(record.getTaxType()));
    tax.setAuthority(Optional.ofNullable(record.getAuthority()));
    tax.setAnnualAmount(MoneyAmount.of(record.getAnnualAmount(), record.getCurrency()));
    tax.setPaymentFrequency(record.getPaymentFrequency());
    tax.setDueMonths(Optional.ofNullable(record.getDueMonths()));
    tax.setTaxYear(Optional.ofNullable(record.getTaxYear()));
    tax.setStartDate(Optional.ofNullable(record.getStartDate()));
    tax.setEndDate(Optional.ofNullable(record.getEndDate()));
    tax.setStatus(PropertyTax.TaxStatus.valueOf(record.getStatus()));
    tax.setNotes(Optional.ofNullable(record.getNotes()));
    tax.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    tax.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    tax.setCreatedBy(record.getCreatedBy());
    tax.setUpdatedBy(record.getUpdatedBy());
    tax.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(tax);
  }
}
