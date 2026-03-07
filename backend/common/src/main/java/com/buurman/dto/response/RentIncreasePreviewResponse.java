package com.buurman.dto.response;

import java.util.List;

public record RentIncreasePreviewResponse(
    int year,
    List<RentIncreaseCountrySummary> countrySummaries,
    List<RentIncreaseContractPreview> contracts) {}
