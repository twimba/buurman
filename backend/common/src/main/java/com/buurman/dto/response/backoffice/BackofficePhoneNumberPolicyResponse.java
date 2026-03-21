package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.buurman.util.Generated;

@Generated
public record BackofficePhoneNumberPolicyResponse(
    Map<String, List<String>> policyMatrix,
    int maxCodesPerHour,
    int verificationCodeExpiryMinutes,
    Optional<Instant> updatedAt,
    Optional<String> updatedBy) {}
