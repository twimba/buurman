package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Ulid;

public record TenantSummary(
    Ulid identifier,
    String firstName,
    String lastName,
    Optional<String> email,
    Optional<String> phone) {}
