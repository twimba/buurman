package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.domain.UnitStatus;
import com.buurman.domain.UnitType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateUnitRequest(
    @NotBlank(message = "Unit number is required") @Size(max = 50, message = "Unit number must be at most 50 characters") String unitNumber,
    Optional<String> name,
    Optional<Integer> floor,
    @NotNull(message = "Unit type is required") UnitType unitType,
    Optional<UnitStatus> status,
    Optional<@Positive(message = "Area value must be positive") BigDecimal> areaValue,
    Optional<String> areaUnit,
    Optional<
            @DecimalMin(value = "0", message = "Allocation share must be at least 0") @DecimalMax(value = "100", message = "Allocation share must be at most 100") BigDecimal>
        allocationShare,
    Optional<
            @DecimalMin(value = "0", message = "WOZ share must be at least 0") @DecimalMax(value = "100", message = "WOZ share must be at most 100") BigDecimal>
        wozSharePct,
    Optional<String> energyEfficiencyRating,
    Optional<String> heatingType) {}
