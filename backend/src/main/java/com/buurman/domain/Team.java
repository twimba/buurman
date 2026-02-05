package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

public class Team {

    private UUID id;
    private String identifier;
    private String name;
    private TeamSettings settings;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public Team() {
    }

    public Team(UUID id, String identifier, String name, TeamSettings settings,
                Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.identifier = identifier;
        this.name = name;
        this.settings = settings;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TeamSettings getSettings() {
        return settings;
    }

    public void setSettings(TeamSettings settings) {
        this.settings = settings;
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
}
