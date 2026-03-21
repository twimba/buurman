package com.buurman.dto.response;

import java.util.List;
import com.buurman.util.Generated;

@Generated
public record BulkImportResult(
    int totalReceived, int totalCreated, int totalFailed, List<String> errors) {}
