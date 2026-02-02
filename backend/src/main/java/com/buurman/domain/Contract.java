package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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

    public Contract() {
    }

    public Contract(UUID id, String identifier, UUID teamId, UUID propertyId, UUID tenantId,
                    ContractType contractType, LocalDate startDate, LocalDate endDate, LocalDate signedDate,
                    BigDecimal rentAmount, BigDecimal depositAmount, BigDecimal securityDeposit, String currency,
                    PaymentFrequency paymentFrequency, Integer paymentDueDay, Boolean autoRenewal,
                    Integer renewalNoticeDays, Integer terminationNoticeDays, BigDecimal lateFeePercentage,
                    ContractStatus status, String termsAndConditions, String notes,
                    Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy, Instant deletedAt) {
        this.id = id;
        this.identifier = identifier;
        this.teamId = teamId;
        this.propertyId = propertyId;
        this.tenantId = tenantId;
        this.contractType = contractType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.signedDate = signedDate;
        this.rentAmount = rentAmount;
        this.depositAmount = depositAmount;
        this.securityDeposit = securityDeposit;
        this.currency = currency;
        this.paymentFrequency = paymentFrequency;
        this.paymentDueDay = paymentDueDay;
        this.autoRenewal = autoRenewal;
        this.renewalNoticeDays = renewalNoticeDays;
        this.terminationNoticeDays = terminationNoticeDays;
        this.lateFeePercentage = lateFeePercentage;
        this.status = status;
        this.termsAndConditions = termsAndConditions;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.deletedAt = deletedAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public UUID getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(UUID propertyId) {
        this.propertyId = propertyId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public void setContractType(ContractType contractType) {
        this.contractType = contractType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getSignedDate() {
        return signedDate;
    }

    public void setSignedDate(LocalDate signedDate) {
        this.signedDate = signedDate;
    }

    public BigDecimal getRentAmount() {
        return rentAmount;
    }

    public void setRentAmount(BigDecimal rentAmount) {
        this.rentAmount = rentAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(BigDecimal securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentFrequency getPaymentFrequency() {
        return paymentFrequency;
    }

    public void setPaymentFrequency(PaymentFrequency paymentFrequency) {
        this.paymentFrequency = paymentFrequency;
    }

    public Integer getPaymentDueDay() {
        return paymentDueDay;
    }

    public void setPaymentDueDay(Integer paymentDueDay) {
        this.paymentDueDay = paymentDueDay;
    }

    public Boolean getAutoRenewal() {
        return autoRenewal;
    }

    public void setAutoRenewal(Boolean autoRenewal) {
        this.autoRenewal = autoRenewal;
    }

    public Integer getRenewalNoticeDays() {
        return renewalNoticeDays;
    }

    public void setRenewalNoticeDays(Integer renewalNoticeDays) {
        this.renewalNoticeDays = renewalNoticeDays;
    }

    public Integer getTerminationNoticeDays() {
        return terminationNoticeDays;
    }

    public void setTerminationNoticeDays(Integer terminationNoticeDays) {
        this.terminationNoticeDays = terminationNoticeDays;
    }

    public BigDecimal getLateFeePercentage() {
        return lateFeePercentage;
    }

    public void setLateFeePercentage(BigDecimal lateFeePercentage) {
        this.lateFeePercentage = lateFeePercentage;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
