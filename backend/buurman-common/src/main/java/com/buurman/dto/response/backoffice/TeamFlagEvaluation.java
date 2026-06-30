package com.buurman.dto.response.backoffice;

import java.util.Map;

import com.buurman.domain.Sid;
import com.buurman.dto.response.FeatureFlagState;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record TeamFlagEvaluation(
    Sid teamIdentifier,
    String teamName,
    String role,
    boolean isOwner,
    boolean isActive,
    Map<String, FeatureFlagState> flags) {}
