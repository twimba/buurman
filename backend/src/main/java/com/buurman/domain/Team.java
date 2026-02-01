package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Team domain object (POJO).
 * Represents a team in the system.
 */
public class Team {

    private UUID id;
    private String businessId;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;

    // Default constructor for MapStruct
    public Team() {
    }

    // All-args constructor
    public Team(UUID id, String businessId, String name, Instant createdAt, Instant updatedAt, UUID createdBy) {
        this.id = id;
        this.businessId = businessId;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
