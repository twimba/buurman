package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;

public record SignatureSignerResponse(
    String email,
    SignatureSignerRole role,
    SignatureSignerStatus status,
    Optional<Instant> signedAt) {}
