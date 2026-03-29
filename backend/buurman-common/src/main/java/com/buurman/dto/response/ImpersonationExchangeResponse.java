package com.buurman.dto.response;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ImpersonationExchangeResponse(
    String token,
    Sid sessionIdentifier,
    String adminEmail,
    String adminName,
    String mode,
    long expiresIn,
    String targetUserEmail,
    Sid targetTeamIdentifier,
    String reason) {}
