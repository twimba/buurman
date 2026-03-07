package com.buurman.dto.request;

import jakarta.validation.constraints.Min;

public record RentIncreasePreviewRequest(@Min(1900) int year) {}
