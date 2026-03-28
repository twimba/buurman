package com.buurman.dto.response;

import java.util.List;
import java.util.Map;

public record ImportUploadResponse(
    List<String> columns,
    List<Map<String, String>> previewRows,
    String fileName,
    int totalRows,
    String fileFormat) {}
