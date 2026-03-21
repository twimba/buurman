package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record TenantSummary(
    Sid identifier,
    String firstName,
    String lastName,
    Optional<String> email,
    Optional<String> phone) {}
