package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Property {

    private UUID id;
    private String identifier;
    private UUID teamId;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal squareMeters;
    private PropertyType propertyType;
    private PropertyStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;

    public Property() {
    }

    public Property(UUID id, String identifier, UUID teamId, String street, String city,
                    String postalCode, String country, Integer bedrooms, Integer bathrooms,
                    BigDecimal squareMeters, PropertyType propertyType, PropertyStatus status,
                    Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy,
                    Instant deletedAt) {
        this.id = id;
        this.identifier = identifier;
        this.teamId = teamId;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.squareMeters = squareMeters;
        this.propertyType = propertyType;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.deletedAt = deletedAt;
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

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
    }

    public BigDecimal getSquareMeters() {
        return squareMeters;
    }

    public void setSquareMeters(BigDecimal squareMeters) {
        this.squareMeters = squareMeters;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public void setStatus(PropertyStatus status) {
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

    public enum PropertyType {
        APARTMENT,
        HOUSE,
        STUDIO,
        COMMERCIAL
    }

    public enum PropertyStatus {
        VACANT,
        OCCUPIED,
        MAINTENANCE,
        UNAVAILABLE
    }
}
