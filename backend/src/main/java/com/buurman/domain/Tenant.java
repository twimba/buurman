package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

public class Tenant {

    private UUID id;
    private String identifier;
    private UUID teamId;
    private String name;
    private String email;
    private String phone;
    private String taxNumber;
    private String idNumber;
    private UUID currentPropertyId;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;

    public Tenant() {
    }

    public Tenant(UUID id, String identifier, UUID teamId, String name, String email,
                  String phone, String taxNumber, String idNumber, UUID currentPropertyId,
                  Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy,
                  Instant deletedAt) {
        this.id = id;
        this.identifier = identifier;
        this.teamId = teamId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.taxNumber = taxNumber;
        this.idNumber = idNumber;
        this.currentPropertyId = currentPropertyId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public UUID getCurrentPropertyId() {
        return currentPropertyId;
    }

    public void setCurrentPropertyId(UUID currentPropertyId) {
        this.currentPropertyId = currentPropertyId;
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
