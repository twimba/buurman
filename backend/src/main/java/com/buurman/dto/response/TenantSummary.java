package com.buurman.dto.response;

import java.util.Optional;

public record TenantSummary(
    String identifier,
    String firstName,
    String lastName,
    Optional<String> email,
    Optional<String> phone) {}
