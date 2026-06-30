package com.buurman.dto.response.backoffice;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record FeatureFlagAdminStatusResponse(boolean adminConfigured, String authMethod) {}
