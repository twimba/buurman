package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.util.Generated;

@Generated
public record TenantPropertyAssignment(PropertySummary property, Optional<String> role) {}
