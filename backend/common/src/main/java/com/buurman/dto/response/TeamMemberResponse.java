package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record TeamMemberResponse(
    Sid userIdentifier,
    String email,
    String name,
    String role,
    boolean isOwner,
    Instant joinedAt,
    boolean isCurrentUser) {}
