package com.buurman.dto.response;

import java.time.Instant;
import java.util.List;

import com.buurman.util.Generated;

@Generated
public record CountryRegulationRequestSummary(
    String countryName,
    int requestCount,
    Instant firstRequestedAt,
    Instant lastRequestedAt,
    List<CountryRegulationRequester> requesters) {}
