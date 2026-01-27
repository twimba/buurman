package com.buurman.dto.response;

import java.time.Instant;

public record InfoResponse(
        String version,
        String environment,
        Instant buildTime
) {}
