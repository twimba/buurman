package com.buurman.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkDownloadRequest(
        @NotEmpty(message = "Document identifiers are required")
        @Size(max = 50, message = "Cannot download more than 50 documents at once")
        List<String> documentIdentifiers
) {
}
