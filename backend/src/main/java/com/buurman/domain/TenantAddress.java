package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

public class TenantAddress {

    public enum AddressType {
        CURRENT,
        MAILING,
        RELATIVE,
        WORK,
        HISTORIC
    }

    public enum AddressStatus {
        ACTIVE,
        INACTIVE
    }

    private UUID id;
    private String identifier;
    private UUID tenantId;
    private UUID teamId;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private AddressType addressType;
    private AddressStatus status;
    private Double latitude;
    private Double longitude;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;

    public TenantAddress() {
    }

    public TenantAddress(UUID id, UUID tenantId, UUID teamId, String street, String city,
                        String postalCode, String country, AddressType addressType, AddressStatus status,
                        Double latitude, Double longitude,
                        Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy, Instant deletedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.teamId = teamId;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.addressType = addressType;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public AddressType getAddressType() {
        return addressType;
    }

    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }

    public AddressStatus getStatus() {
        return status;
    }

    public void setStatus(AddressStatus status) {
        this.status = status;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
