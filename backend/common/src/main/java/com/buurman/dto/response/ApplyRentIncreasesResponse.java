package com.buurman.dto.response;

import java.util.List;

public record ApplyRentIncreasesResponse(
    List<RentIncreaseResult> results, RentIncreaseSummary summary) {}
