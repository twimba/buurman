package com.buurman.dto.response.backoffice;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PhonePolicyMetadataResponse(
    List<CountryGroupResponse> countryGroups, List<String> numberTypes) {}
