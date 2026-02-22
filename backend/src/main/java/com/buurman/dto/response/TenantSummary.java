package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

public record TenantSummary(
    String identifier,
    String firstName,
    String lastName,
    @Nullable String email,
    @Nullable String phone) {}
