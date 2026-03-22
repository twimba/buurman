package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.FinancingPayment;
import com.buurman.jooq.generated.tables.records.FinancingPaymentsRecord;
import com.buurman.util.MoneyAmount;

@Component
public class FinancingPaymentRecordMapper {

  public Optional<FinancingPayment> toDomain(@Nullable FinancingPaymentsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    FinancingPayment payment = new FinancingPayment();
    payment.setId(record.getId());
    payment.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    payment.setFinancingId(record.getFinancingId());
    payment.setTeamId(record.getTeamId());
    payment.setPaymentDate(record.getPaymentDate());
    payment.setTotalAmount(MoneyAmount.of(record.getTotalAmount(), record.getCurrency()));
    payment.setPrincipalAmount(Optional.ofNullable(record.getPrincipalAmount()));
    payment.setInterestAmount(Optional.ofNullable(record.getInterestAmount()));
    payment.setEscrowAmount(Optional.ofNullable(record.getEscrowAmount()));
    payment.setExtraPayment(Optional.ofNullable(record.getExtraPayment()));
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
