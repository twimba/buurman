package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record BackofficePhoneNumberPolicyResponse(
    Map<String, List<String>> policyMatrix,
    int maxCodesPerHour,
    int verificationCodeExpiryMinutes,
    Instant updatedAt,
    String updatedBy
) {}
