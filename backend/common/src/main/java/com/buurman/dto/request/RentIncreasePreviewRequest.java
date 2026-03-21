package com.buurman.dto.request;

import jakarta.validation.constraints.Min;
import com.buurman.util.Generated;

@Generated
public record RentIncreasePreviewRequest(@Min(1900) int year) {}
