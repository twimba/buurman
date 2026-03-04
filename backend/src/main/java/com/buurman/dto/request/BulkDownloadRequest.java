package com.buurman.dto.request;

import java.util.List;

import com.buurman.domain.Sid;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkDownloadRequest(
    @NotEmpty(message = "Document identifiers are required") @Size(max = 50, message = "Cannot download more than 50 documents at once") List<Sid> documentIdentifiers) {}
