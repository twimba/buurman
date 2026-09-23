package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.buurman.util.MoneyAmount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceival {

  /** How the balance was settled: money received, written off, or an applied tenant credit. */
  public enum ReceivalType {
    PAYMENT,
    WRITE_OFF,
    CREDIT
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID paymentId;
  private MoneyAmount amount;
  private LocalDate receivalDate;
  @Builder.Default private Optional<String> notes = Optional.empty();
  @Builder.Default private ReceivalType receivalType = ReceivalType.PAYMENT;
  @Builder.Default private Optional<UUID> creditId = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
