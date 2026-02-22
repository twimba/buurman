package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

public record BackofficePhoneNumberPolicyResponse(
    Map<String, List<String>> policyMatrix,
    int maxCodesPerHour,
    int verificationCodeExpiryMinutes,
    @Nullable Instant updatedAt,
    @Nullable String updatedBy) {}
