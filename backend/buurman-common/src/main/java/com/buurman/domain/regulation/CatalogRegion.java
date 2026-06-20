package com.buurman.domain.regulation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** A region entry within a {@link CatalogCountry}. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogRegion(String regionCode, String regionName, String summary) {}
