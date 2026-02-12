package com.buurman.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ContractPaymentInstruction {

    private UUID id;
    private String identifier;
    private UUID teamId;
    private UUID contractId;
    private UUID paymentInstructionId;
    private Boolean isCustom;
    private String customName;
    private String customDescription;
    private String customPaymentMethod;
    private String customBankName;
    private String customAccountHolderName;
    private String customIban;
    private String customBicSwift;
    private String customAccountNumber;
    private String customRoutingNumber;
    private String customPaymentReference;
    private String customAdditionalDetails;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;
}
