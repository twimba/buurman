package com.buurman.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
public class PaymentReceival {

    private UUID id;
    private String identifier;
    private UUID teamId;
    private UUID paymentId;
    private BigDecimal amount;
    private LocalDate receivalDate;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;
}
