package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Sid;

public record TakeoutResponse(
    Sid identifier,
    String status,
    int progress,
    @Nullable Long fileSize,
    Instant createdAt,
    @Nullable Instant completedAt,
    @Nullable Instant expiresAt,
    @Nullable String downloadUrl) {}
