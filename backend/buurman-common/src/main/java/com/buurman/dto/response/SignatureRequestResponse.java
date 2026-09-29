package com.buurman.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequestStatus;

public record SignatureRequestResponse(
    Sid identifier,
    Sid documentIdentifier,
    Optional<Sid> signedDocumentIdentifier,
    SignatureRequestStatus status,
    List<SignatureSignerResponse> signers,
    Instant createdAt,
    Instant updatedAt) {}
