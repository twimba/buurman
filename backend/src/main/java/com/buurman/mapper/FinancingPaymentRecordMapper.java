package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.FinancingPayment;
import com.buurman.jooq.generated.tables.records.FinancingPaymentsRecord;
import com.buurman.util.CurrencyUtils;

@Component
public class FinancingPaymentRecordMapper {

  public Optional<FinancingPayment> toDomain(@Nullable FinancingPaymentsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    String currency = record.getCurrency();

    FinancingPayment payment = new FinancingPayment();
    payment.setId(record.getId());
    payment.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    payment.setFinancingId(record.getFinancingId());
    payment.setTeamId(record.getTeamId());
    payment.setPaymentDate(record.getPaymentDate());
    payment.setTotalAmount(CurrencyUtils.toMajorUnits(record.getTotalAmount(), currency));
    payment.setPrincipalAmount(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(record.getPrincipalAmount(), currency)));
    payment.setInterestAmount(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(record.getInterestAmount(), currency)));
    payment.setEscrowAmount(
        Optional.ofNullable(CurrencyUtils.toMajorUnitsOrNull(record.getEscrowAmount(), currency)));
    payment.setExtraPayment(
        Optional.ofNullable(CurrencyUtils.toMajorUnitsOrNull(record.getExtraPayment(), currency)));
    payment.setCurrency(currency);
    payment.setStatus(FinancingPayment.PaymentStatus.valueOf(record.getStatus()));
    payment.setNotes(Optional.ofNullable(record.getNotes()));
    payment.setBalanceDeducted(record.getBalanceDeducted());
    payment.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    payment.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    payment.setCreatedBy(record.getCreatedBy());
    payment.setUpdatedBy(record.getUpdatedBy());
    payment.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(payment);
  }
}
