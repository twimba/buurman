package com.buurman.domain.regulation;

import com.buurman.domain.TenancyRuleTopic;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A display-only tenancy-law reference entry within a {@link CatalogCountry}.
 *
 * <p>These are facts that are not rent-increase caps — notice periods, tenancy duration, deposits,
 * lease formalities, registration duties, fixed-amount fees. Nothing computes off them; they are
 * rendered as reference material.
 *
 * <p>{@code regionCode} links the entry to a {@link CatalogRegion} of the same country; {@code
 * null} denotes a national rule. Only the CURRENT rule is stored — a superseded entry is replaced,
 * not retained, so {@code effectiveFrom} dates the present fact rather than opening a history.
 *
 * <p>{@code value} is free text ("5 years", "DKK 344", "2 months' rent", "Mandatory, written")
 * because the facts are heterogeneous and no unit system fits them; a topic that later needs to
 * drive behaviour is promoted to a typed column instead, as {@code formalNoticeDays} is.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogTenancyRule(
    TenancyRuleTopic topic,
    String regionCode,
    String label,
    String value,
    String effectiveFrom,
    String legalBasis,
    String sourceUrl,
    String notes) {}
