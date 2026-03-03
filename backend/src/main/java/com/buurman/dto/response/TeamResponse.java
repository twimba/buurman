package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.Ulid;

public record TeamResponse(
    Ulid identifier, String teamName, long memberCount, Instant createdAt) {}
