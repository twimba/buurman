package com.buurman.dto.response;

import java.util.List;

public record ImportPreviewResponse(
    int toCreate,
    int toSkip,
    int errors,
    int totalRows,
    List<ImportPreviewItemResponse> items) {}
