package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

public record TenantPropertyAssignment(PropertySummary property, @Nullable String role) {}
