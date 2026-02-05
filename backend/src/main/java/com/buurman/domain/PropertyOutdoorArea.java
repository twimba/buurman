package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PropertyOutdoorArea {

    private UUID id;
    private String identifier;
    private UUID propertyId;
    private UUID teamId;
    private String type;
    private BigDecimal areaValue;
    private String areaUnit;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;

    public PropertyOutdoorArea() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public UUID getPropertyId() { return propertyId; }
    public void setPropertyId(UUID propertyId) { this.propertyId = propertyId; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAreaValue() { return areaValue; }
    public void setAreaValue(BigDecimal areaValue) { this.areaValue = areaValue; }

    public String getAreaUnit() { return areaUnit; }
    public void setAreaUnit(String areaUnit) { this.areaUnit = areaUnit; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
