package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.FinancingPayment;
import com.buurman.domain.FinancingPayment.PaymentStatus;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.FinancingPaymentsRecord;

@DisplayName("FinancingPaymentRecordMapper")
class FinancingPaymentRecordMapperTest {

  private final FinancingPaymentRecordMapper mapper = new FinancingPaymentRecordMapper();

  private static final UUID ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID FINANCING_ID = UUID.randomUUID();
  private static final UUID CREATED_BY = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();
  private static final Sid IDENTIFIER = Sid.of("FPY01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 12, 0, 0);

  @Nested
  @DisplayName("toDomain")
  class ToDomain {

    @Test
    @DisplayName("returns empty Optional for null record")
    void returnsEmptyForNull() {
      Optional<FinancingPayment> result = mapper.toDomain(null);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("maps all fields from a complete record")
    void mapsCompleteRecord() {
      FinancingPaymentsRecord record = createCompleteRecord();

      Optional<FinancingPayment> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      FinancingPayment payment = result.get();
      assertThat(payment.getId()).isEqualTo(ID);
      assertThat(payment.getIdentifier()).contains(IDENTIFIER);
      assertThat(payment.getFinancingId()).isEqualTo(FINANCING_ID);
      assertThat(payment.getTeamId()).isEqualTo(TEAM_ID);
      assertThat(payment.getPaymentDate()).isEqualTo(LocalDate.of(2026, 3, 1));
      assertThat(payment.getTotalAmount().value()).isEqualByComparingTo(new BigDecimal("1500.00"));
      assertThat(payment.getTotalAmount().currency()).isEqualTo("EUR");
      assertThat(payment.getPrincipalAmount()).contains(new BigDecimal("800.00"));
      assertThat(payment.getInterestAmount()).contains(new BigDecimal("600.00"));
      assertThat(payment.getEscrowAmount()).contains(new BigDecimal("100.00"));
      assertThat(payment.getExtraPayment()).contains(new BigDecimal("0.00"));
      assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
      assertThat(payment.getNotes()).contains("Monthly payment");
      assertThat(payment.isBalanceDeducted()).isTrue();
      assertThat(payment.getCreatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(payment.getUpdatedAt()).isEqualTo(NOW.toInstant(ZoneOffset.UTC));
      assertThat(payment.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("maps all PaymentStatus enum values")
    void mapsAllPaymentStatuses() {
      for (PaymentStatus status : PaymentStatus.values()) {
        FinancingPaymentsRecord record = createCompleteRecord();
        record.setStatus(status.name());

        Optional<FinancingPayment> result = mapper.toDomain(record);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(status);
      }
    }

    @Test
    @DisplayName("wraps nullable fields as empty Optional when null")
    void wrapsNullableFieldsAsEmpty() {
      FinancingPaymentsRecord record = createCompleteRecord();
      record.setPrincipalAmount(null);
      record.setInterestAmount(null);
      record.setEscrowAmount(null);
      record.setExtraPayment(null);
      record.setNotes(null);

      Optional<FinancingPayment> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      FinancingPayment payment = result.get();
      assertThat(payment.getPrincipalAmount()).isEmpty();
      assertThat(payment.getInterestAmount()).isEmpty();
      assertThat(payment.getEscrowAmount()).isEmpty();
      assertThat(payment.getExtraPayment()).isEmpty();
      assertThat(payment.getNotes()).isEmpty();
    }

    @Test
    @DisplayName("maps deletedAt when present")
    void mapsDeletedAt() {
      FinancingPaymentsRecord record = createCompleteRecord();
      LocalDateTime deletedAt = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
      record.setDeletedAt(deletedAt);

      Optional<FinancingPayment> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().getDeletedAt()).contains(deletedAt.toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("maps balanceDeducted false correctly")
    void mapsBalanceDeductedFalse() {
      FinancingPaymentsRecord record = createCompleteRecord();
      record.setBalanceDeducted(false);

      Optional<FinancingPayment> result = mapper.toDomain(record);

      assertThat(result).isPresent();
      assertThat(result.get().isBalanceDeducted()).isFalse();
    }
  }

  private FinancingPaymentsRecord createCompleteRecord() {
    FinancingPaymentsRecord record = new FinancingPaymentsRecord();
    record.setId(ID);
    record.setIdentifier(IDENTIFIER);
    record.setFinancingId(FINANCING_ID);
    record.setTeamId(TEAM_ID);
    record.setPaymentDate(LocalDate.of(2026, 3, 1));
    record.setTotalAmount(new BigDecimal("1500.00"));
    record.setCurrency("EUR");
    record.setPrincipalAmount(new BigDecimal("800.00"));
    record.setInterestAmount(new BigDecimal("600.00"));
    record.setEscrowAmount(new BigDecimal("100.00"));
    record.setExtraPayment(new BigDecimal("0.00"));
    record.setStatus("COMPLETED");
    record.setNotes("Monthly payment");
    record.setBalanceDeducted(true);
    record.setCreatedAt(NOW);
    record.setUpdatedAt(NOW);
    record.setCreatedBy(CREATED_BY);
    record.setUpdatedBy(UPDATED_BY);
    record.setDeletedAt(null);
    return record;
  }
}
