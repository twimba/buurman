package com.buurman.dto.response;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ImpersonationSessionInfo(
    Sid sessionIdentifier,
    String adminEmail,
    String adminName,
    String mode,
    long remainingSeconds,
    String targetUserEmail,
    String reason) {}
