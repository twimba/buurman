package com.buurman.domain.regulation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** A single country entry within the {@link RentRegulationCatalog}. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogCountry(
    String countryCode,
    String countryName,
    boolean hasRegionalRegulations,
    String summary,
    String lastReviewedAt,
    List<CatalogRegion> regions,
    List<CatalogRule> rules,
    CatalogLateFee lateFee) {}
