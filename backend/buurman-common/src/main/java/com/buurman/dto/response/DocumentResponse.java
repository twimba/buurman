package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record DocumentResponse(
    Sid identifier,
    String entityType,
    Sid entityIdentifier,
    String fileKey,
    String fileName,
    Optional<Long> fileSize,
    Optional<String> mimeType,
    Optional<String> title,
    Optional<String> notes,
    // The original document this one was generated from (e.g. a signed copy or signing
    // certificate), so the UI can group it under that document instead of showing it as an
    // unrelated row. Empty for everything else, including the original itself.
    Optional<Sid> sourceDocumentIdentifier,
    Instant uploadedAt,
    Optional<String> downloadUrl) {}
