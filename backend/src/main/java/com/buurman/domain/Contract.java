package com.buurman.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    public enum ContractType {
        FIXED_TERM,
        INDEFINITE,
        FURNISHED,
        UNFURNISHED
    }

    public enum PaymentFrequency {
        MONTHLY,
        QUARTERLY,
        ANNUALLY
    }

    public enum ContractStatus {
        DRAFT,
        ACTIVE,
        EXPIRED,
        TERMINATED,
        PENDING_SIGNATURE
    }

    private UUID id;
    private String identifier;
    private UUID teamId;
    private UUID propertyId;
    private UUID tenantId;
    private ContractType contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate signedDate;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private BigDecimal securityDeposit;
    private String currency;
    private PaymentFrequency paymentFrequency;
    private Integer paymentDueDay;
    private Boolean autoRenewal;
    private Integer renewalNoticeDays;
    private Integer terminationNoticeDays;
    private BigDecimal lateFeePercentage;
    private ContractStatus status;
    private String termsAndConditions;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;
}
