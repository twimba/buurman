package com.buurman.dto.request.backoffice;

import org.jspecify.annotations.Nullable;

public record UpdateFeatureFlagRequest(@Nullable Boolean enabled, @Nullable String value) {}
