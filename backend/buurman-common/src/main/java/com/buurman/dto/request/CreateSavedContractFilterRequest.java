package com.buurman.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSavedContractFilterRequest(
    @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must be at most 100 characters") String name,
    @NotNull(message = "Criteria is required") Map<String, Object> criteria) {}
