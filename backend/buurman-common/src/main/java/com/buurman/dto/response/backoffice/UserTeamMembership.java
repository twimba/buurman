package com.buurman.dto.response.backoffice;

import java.time.Instant;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record UserTeamMembership(
    Sid teamIdentifier,
    String teamName,
    String role,
    boolean isOwner,
    boolean demo,
    Instant joinedAt) {}
