package com.buurman.dto.response;

import com.buurman.domain.Sid;

public record DataImportItemResponse(
    Sid entityIdentifier, String entityType, String displayName, int rowNumber) {}
