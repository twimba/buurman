package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record TeamResponse(
    Sid identifier, String teamName, long memberCount, int billableUnitCount, Instant createdAt) {}
