package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Sid;

/**
 * The stored message of one communication, as the recipient received it.
 *
 * <p>Separate from {@link CommunicationResponse} rather than a field on it: a hundred-row timeline
 * would otherwise pull a hundred rendered emails to draw five lines of metadata each. A preview is
 * opened for one row at a time, so the body is fetched for one row at a time.
 *
 * <p>Carries no delivery metadata. Everything else a preview shows — type, status, recipient, when
 * — is already in the row the caller is looking at.
 */
public record CommunicationBodyResponse(
    Sid identifier, String channel, Optional<String> subject, String body) {}
