package com.buurman.dto.response;

import com.buurman.domain.Sid;

public record ImportExecuteResponse(
    Sid identifier, int importedCount, int skippedCount, int errorCount, int totalRows) {}
