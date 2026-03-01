package com.buurman.dto.response.backoffice;

import org.jspecify.annotations.Nullable;

public record FeatureFlagUpdateResponse(
    String flagName, boolean enabled, @Nullable Object value) {}
