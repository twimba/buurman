package com.buurman.dto.response;

import java.util.List;

public record BulkImportResult(
    int totalReceived,
    int totalCreated,
    int totalFailed,
    List<String> errors) {}
