package com.buurman.dto.response;

import java.util.Optional;

public record ImportPreviewItemResponse(
    int rowNumber,
    String status,
    String displayName,
    Optional<String> email,
    Optional<String> phone,
    Optional<String> errorMessage,
    Optional<String> duplicateOfIdentifier,
    Optional<String> duplicateOfDisplayName) {}
