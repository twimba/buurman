package com.buurman.dto.response;

import java.util.UUID;

public record TenantSummary(
        UUID id,
        String identifier,
        String firstName,
        String lastName,
        String email,
        String phone
) {}
