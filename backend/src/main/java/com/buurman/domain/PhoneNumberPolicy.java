package com.buurman.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhoneNumberPolicy {

    private UUID id;
    private Map<String, List<String>> policyMatrix; // country code -> allowed number types
    private int maxCodesPerHour = 3;
    private int verificationCodeExpiryMinutes = 10;
    private Instant updatedAt;
    private String updatedBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Map<String, List<String>> getPolicyMatrix() {
        return policyMatrix;
    }

    public void setPolicyMatrix(Map<String, List<String>> policyMatrix) {
        this.policyMatrix = policyMatrix;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public int getMaxCodesPerHour() {
        return maxCodesPerHour;
    }

    public void setMaxCodesPerHour(int maxCodesPerHour) {
        this.maxCodesPerHour = maxCodesPerHour;
    }

    public int getVerificationCodeExpiryMinutes() {
        return verificationCodeExpiryMinutes;
    }

    public void setVerificationCodeExpiryMinutes(int verificationCodeExpiryMinutes) {
        this.verificationCodeExpiryMinutes = verificationCodeExpiryMinutes;
    }

    public boolean isAllowed(String countryCode, String numberType) {
        if (policyMatrix == null) return false;
        List<String> allowedTypes = policyMatrix.get(countryCode);
        if (allowedTypes == null || allowedTypes.isEmpty()) return false;
        if (numberType == null) return true; // type unknown but country is allowed
        return allowedTypes.contains(numberType);
    }
}
