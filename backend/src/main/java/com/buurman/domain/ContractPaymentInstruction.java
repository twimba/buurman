package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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

    public ContractPaymentInstruction() {
    }

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

    public UUID getContractId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }

    public UUID getPaymentInstructionId() {
        return paymentInstructionId;
    }

    public void setPaymentInstructionId(UUID paymentInstructionId) {
        this.paymentInstructionId = paymentInstructionId;
    }

    public Boolean getIsCustom() {
        return isCustom;
    }

    public void setIsCustom(Boolean isCustom) {
        this.isCustom = isCustom;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getCustomDescription() {
        return customDescription;
    }

    public void setCustomDescription(String customDescription) {
        this.customDescription = customDescription;
    }

    public String getCustomPaymentMethod() {
        return customPaymentMethod;
    }

    public void setCustomPaymentMethod(String customPaymentMethod) {
        this.customPaymentMethod = customPaymentMethod;
    }

    public String getCustomBankName() {
        return customBankName;
    }

    public void setCustomBankName(String customBankName) {
        this.customBankName = customBankName;
    }

    public String getCustomAccountHolderName() {
        return customAccountHolderName;
    }

    public void setCustomAccountHolderName(String customAccountHolderName) {
        this.customAccountHolderName = customAccountHolderName;
    }

    public String getCustomIban() {
        return customIban;
    }

    public void setCustomIban(String customIban) {
        this.customIban = customIban;
    }

    public String getCustomBicSwift() {
        return customBicSwift;
    }

    public void setCustomBicSwift(String customBicSwift) {
        this.customBicSwift = customBicSwift;
    }

    public String getCustomAccountNumber() {
        return customAccountNumber;
    }

    public void setCustomAccountNumber(String customAccountNumber) {
        this.customAccountNumber = customAccountNumber;
    }

    public String getCustomRoutingNumber() {
        return customRoutingNumber;
    }

    public void setCustomRoutingNumber(String customRoutingNumber) {
        this.customRoutingNumber = customRoutingNumber;
    }

    public String getCustomPaymentReference() {
        return customPaymentReference;
    }

    public void setCustomPaymentReference(String customPaymentReference) {
        this.customPaymentReference = customPaymentReference;
    }

    public String getCustomAdditionalDetails() {
        return customAdditionalDetails;
    }

    public void setCustomAdditionalDetails(String customAdditionalDetails) {
        this.customAdditionalDetails = customAdditionalDetails;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
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
