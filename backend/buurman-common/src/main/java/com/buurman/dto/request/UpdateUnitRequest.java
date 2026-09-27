package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * {@code allocationShare} and {@code wozSharePct} are deliberately absent here — {@code PUT
 * /properties/{id}/allocation} ({@link com.buurman.service.PropertyService#updateAllocation}) is
 * their one designed writer, and it validates property membership before touching them. Every field
 * in this record is {@link Optional} and never null, so an omitted field clears the column; if
 * these two were included, editing any other unit field through this endpoint (e.g. its energy
 * label) would silently null out its allocation share, and a later recompute would redistribute
 * that unit's share across its siblings onto a legal statement.
 */
public record UpdateUnitRequest(
    @NotBlank(message = "Unit number is required") @Size(max = 50, message = "Unit number must be at most 50 characters") String unitNumber,
    Optional<String> name,
    Optional<Integer> floor,
    @NotNull(message = "Unit type is required") UnitType unitType,
    Optional<UnitStatus> status,
    Optional<@Positive(message = "Area value must be positive") BigDecimal> areaValue,
    Optional<String> areaUnit,
    Optional<@Positive(message = "WOZ value must be positive") BigDecimal> wozValue,
    Optional<String> wozValueCurrency,

    // Energy & Climate
    Optional<String> energyEfficiencyRating,
    Optional<LocalDate> energyCertificateExpiryDate,
    Optional<String> heatingType,
    Optional<String> coolingType,
    Optional<String> hotWaterSystem,
    Optional<String> insulationNotes,

    // Finishes
    Optional<String> flooringType,
    Optional<String> windowType,

    // Safety
    Optional<Boolean> hasSmokeDetectors,
    Optional<Boolean> hasCoDetectors,
    Optional<Boolean> hasFireExtinguisher,

    // Accessibility
    Optional<Boolean> hasAdaptedBathroom,
    Optional<String> accessibilityNotes) {}
