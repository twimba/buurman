package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Property {

    private UUID id;
    private String identifier;
    private UUID teamId;
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal areaValue;
    private String areaUnit;
    private PropertyType propertyType;
    private PropertyStatus status;

    // Construction & Structure
    private Integer yearBuilt;
    private Integer yearLastRenovated;
    private String constructionType;
    private String foundationType;
    private String roofType;
    private String wallConstruction;
    private String flooringType;
    private String windowType;
    private Integer numberOfFloors;
    private String structuralNotes;

    // Energy & Climate
    private String energyEfficiencyRating;
    private LocalDate energyCertificateExpiryDate;
    private String heatingType;
    private String coolingType;
    private String hotWaterSystem;
    private String insulationNotes;

    // Utilities & Connections
    private String electricityConnectionType;
    private Integer electricityCapacityAmps;
    private String waterConnectionType;
    private Boolean hasGasConnection;
    private String sewageType;
    private String internetConnectionType;
    private Integer internetMaxSpeedMbps;
    private String internetStatus;

    // Parking
    private Integer parkingSpaces;
    private String parkingType;

    // Safety & Security
    private Boolean hasSmokeDetectors;
    private Boolean hasCoDetectors;
    private Boolean hasFireExtinguisher;
    private Boolean hasSprinklerSystem;
    private Boolean hasAlarmSystem;
    private Boolean hasSecurityCameras;
    private Boolean hasSecureEntry;
    private String safetyNotes;

    // Accessibility
    private Boolean isWheelchairAccessible;
    private Boolean hasElevator;
    private Boolean hasStepFreeEntrance;
    private Boolean hasAdaptedBathroom;
    private String accessibilityNotes;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;

    public Property() {
    }

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }

    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }

    public BigDecimal getAreaValue() { return areaValue; }
    public void setAreaValue(BigDecimal areaValue) { this.areaValue = areaValue; }

    public String getAreaUnit() { return areaUnit; }
    public void setAreaUnit(String areaUnit) { this.areaUnit = areaUnit; }

    public PropertyType getPropertyType() { return propertyType; }
    public void setPropertyType(PropertyType propertyType) { this.propertyType = propertyType; }

    public PropertyStatus getStatus() { return status; }
    public void setStatus(PropertyStatus status) { this.status = status; }

    public Integer getYearBuilt() { return yearBuilt; }
    public void setYearBuilt(Integer yearBuilt) { this.yearBuilt = yearBuilt; }

    public Integer getYearLastRenovated() { return yearLastRenovated; }
    public void setYearLastRenovated(Integer yearLastRenovated) { this.yearLastRenovated = yearLastRenovated; }

    public String getConstructionType() { return constructionType; }
    public void setConstructionType(String constructionType) { this.constructionType = constructionType; }

    public String getFoundationType() { return foundationType; }
    public void setFoundationType(String foundationType) { this.foundationType = foundationType; }

    public String getRoofType() { return roofType; }
    public void setRoofType(String roofType) { this.roofType = roofType; }

    public String getWallConstruction() { return wallConstruction; }
    public void setWallConstruction(String wallConstruction) { this.wallConstruction = wallConstruction; }

    public String getFlooringType() { return flooringType; }
    public void setFlooringType(String flooringType) { this.flooringType = flooringType; }

    public String getWindowType() { return windowType; }
    public void setWindowType(String windowType) { this.windowType = windowType; }

    public Integer getNumberOfFloors() { return numberOfFloors; }
    public void setNumberOfFloors(Integer numberOfFloors) { this.numberOfFloors = numberOfFloors; }

    public String getStructuralNotes() { return structuralNotes; }
    public void setStructuralNotes(String structuralNotes) { this.structuralNotes = structuralNotes; }

    public String getEnergyEfficiencyRating() { return energyEfficiencyRating; }
    public void setEnergyEfficiencyRating(String energyEfficiencyRating) { this.energyEfficiencyRating = energyEfficiencyRating; }

    public LocalDate getEnergyCertificateExpiryDate() { return energyCertificateExpiryDate; }
    public void setEnergyCertificateExpiryDate(LocalDate energyCertificateExpiryDate) { this.energyCertificateExpiryDate = energyCertificateExpiryDate; }

    public String getHeatingType() { return heatingType; }
    public void setHeatingType(String heatingType) { this.heatingType = heatingType; }

    public String getCoolingType() { return coolingType; }
    public void setCoolingType(String coolingType) { this.coolingType = coolingType; }

    public String getHotWaterSystem() { return hotWaterSystem; }
    public void setHotWaterSystem(String hotWaterSystem) { this.hotWaterSystem = hotWaterSystem; }

    public String getInsulationNotes() { return insulationNotes; }
    public void setInsulationNotes(String insulationNotes) { this.insulationNotes = insulationNotes; }

    public String getElectricityConnectionType() { return electricityConnectionType; }
    public void setElectricityConnectionType(String electricityConnectionType) { this.electricityConnectionType = electricityConnectionType; }

    public Integer getElectricityCapacityAmps() { return electricityCapacityAmps; }
    public void setElectricityCapacityAmps(Integer electricityCapacityAmps) { this.electricityCapacityAmps = electricityCapacityAmps; }

    public String getWaterConnectionType() { return waterConnectionType; }
    public void setWaterConnectionType(String waterConnectionType) { this.waterConnectionType = waterConnectionType; }

    public Boolean getHasGasConnection() { return hasGasConnection; }
    public void setHasGasConnection(Boolean hasGasConnection) { this.hasGasConnection = hasGasConnection; }

    public String getSewageType() { return sewageType; }
    public void setSewageType(String sewageType) { this.sewageType = sewageType; }

    public String getInternetConnectionType() { return internetConnectionType; }
    public void setInternetConnectionType(String internetConnectionType) { this.internetConnectionType = internetConnectionType; }

    public Integer getInternetMaxSpeedMbps() { return internetMaxSpeedMbps; }
    public void setInternetMaxSpeedMbps(Integer internetMaxSpeedMbps) { this.internetMaxSpeedMbps = internetMaxSpeedMbps; }

    public String getInternetStatus() { return internetStatus; }
    public void setInternetStatus(String internetStatus) { this.internetStatus = internetStatus; }

    public Integer getParkingSpaces() { return parkingSpaces; }
    public void setParkingSpaces(Integer parkingSpaces) { this.parkingSpaces = parkingSpaces; }

    public String getParkingType() { return parkingType; }
    public void setParkingType(String parkingType) { this.parkingType = parkingType; }

    public Boolean getHasSmokeDetectors() { return hasSmokeDetectors; }
    public void setHasSmokeDetectors(Boolean hasSmokeDetectors) { this.hasSmokeDetectors = hasSmokeDetectors; }

    public Boolean getHasCoDetectors() { return hasCoDetectors; }
    public void setHasCoDetectors(Boolean hasCoDetectors) { this.hasCoDetectors = hasCoDetectors; }

    public Boolean getHasFireExtinguisher() { return hasFireExtinguisher; }
    public void setHasFireExtinguisher(Boolean hasFireExtinguisher) { this.hasFireExtinguisher = hasFireExtinguisher; }

    public Boolean getHasSprinklerSystem() { return hasSprinklerSystem; }
    public void setHasSprinklerSystem(Boolean hasSprinklerSystem) { this.hasSprinklerSystem = hasSprinklerSystem; }

    public Boolean getHasAlarmSystem() { return hasAlarmSystem; }
    public void setHasAlarmSystem(Boolean hasAlarmSystem) { this.hasAlarmSystem = hasAlarmSystem; }

    public Boolean getHasSecurityCameras() { return hasSecurityCameras; }
    public void setHasSecurityCameras(Boolean hasSecurityCameras) { this.hasSecurityCameras = hasSecurityCameras; }

    public Boolean getHasSecureEntry() { return hasSecureEntry; }
    public void setHasSecureEntry(Boolean hasSecureEntry) { this.hasSecureEntry = hasSecureEntry; }

    public String getSafetyNotes() { return safetyNotes; }
    public void setSafetyNotes(String safetyNotes) { this.safetyNotes = safetyNotes; }

    public Boolean getIsWheelchairAccessible() { return isWheelchairAccessible; }
    public void setIsWheelchairAccessible(Boolean isWheelchairAccessible) { this.isWheelchairAccessible = isWheelchairAccessible; }

    public Boolean getHasElevator() { return hasElevator; }
    public void setHasElevator(Boolean hasElevator) { this.hasElevator = hasElevator; }

    public Boolean getHasStepFreeEntrance() { return hasStepFreeEntrance; }
    public void setHasStepFreeEntrance(Boolean hasStepFreeEntrance) { this.hasStepFreeEntrance = hasStepFreeEntrance; }

    public Boolean getHasAdaptedBathroom() { return hasAdaptedBathroom; }
    public void setHasAdaptedBathroom(Boolean hasAdaptedBathroom) { this.hasAdaptedBathroom = hasAdaptedBathroom; }

    public String getAccessibilityNotes() { return accessibilityNotes; }
    public void setAccessibilityNotes(String accessibilityNotes) { this.accessibilityNotes = accessibilityNotes; }

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
