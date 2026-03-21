package com.buurman.dto.response.backoffice;

import org.jspecify.annotations.Nullable;
import com.buurman.util.Generated;

@Generated
public record FeatureFlagUpdateResponse(String flagName, boolean enabled, @Nullable Object value) {}
