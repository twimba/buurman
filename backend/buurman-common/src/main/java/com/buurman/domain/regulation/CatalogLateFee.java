package com.buurman.domain.regulation;

import java.math.BigDecimal;

import com.buurman.domain.LateFeePolicy;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Late-fee policy for a catalogue country. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogLateFee(LateFeePolicy policy, BigDecimal maxPercentage, String notes) {}
