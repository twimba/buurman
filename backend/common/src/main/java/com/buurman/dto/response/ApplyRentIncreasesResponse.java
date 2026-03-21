package com.buurman.dto.response;

import java.util.List;
import com.buurman.util.Generated;

@Generated
public record ApplyRentIncreasesResponse(
    List<RentIncreaseResult> results, RentIncreaseSummary summary) {}
