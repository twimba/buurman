package com.buurman.dto.response;

import java.util.List;
import com.buurman.util.Generated;

@Generated
public record RentIncreaseCountrySummary(
    String countryCode,
    String countryName,
    int contractCount,
    boolean hasRegulationData,
    List<RentRegulationRuleResponse> rules) {}
