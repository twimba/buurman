package com.buurman.dto.request;

import com.buurman.util.Generated;

import jakarta.validation.constraints.Min;

@Generated
public record RentIncreasePreviewRequest(@Min(1900) int year) {}
