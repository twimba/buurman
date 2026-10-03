package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;

/**
 * One signer's entry in the live "signing links" view. {@code signingUrl} is a bearer credential:
 * it is never stored, never logged, and empty for a signer who can no longer use it.
 */
public record SignatureSigningLinkResponse(
    String name,
    String email,
    SignatureSignerRole role,
    SignatureSignerStatus status,
    Optional<String> signingUrl,
    boolean signed) {}
