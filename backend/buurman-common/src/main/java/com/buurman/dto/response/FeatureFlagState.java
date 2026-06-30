package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

public record FeatureFlagState(boolean enabled, @Nullable Object value) {}
