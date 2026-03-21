package com.buurman.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import com.buurman.util.Generated;

@Generated
public record ApplyRentIncreasesRequest(
    @Min(1900) int year, @NotEmpty @Valid List<RentIncreaseItem> increases) {}
