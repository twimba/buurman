package com.buurman.dto.response;

import java.util.List;

import com.buurman.util.Generated;

@Generated
public record RentIncreasePreviewResponse(
    int year,
    List<RentIncreaseCountrySummary> countrySummaries,
    List<RentIncreaseContractPreview> contracts) {}
