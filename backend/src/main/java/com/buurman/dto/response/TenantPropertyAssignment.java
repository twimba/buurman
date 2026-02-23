package com.buurman.dto.response;

import java.util.Optional;

public record TenantPropertyAssignment(PropertySummary property, Optional<String> role) {}
