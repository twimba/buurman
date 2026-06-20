package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/**
 * Geographic distribution by team default country (ISO 3166-1 alpha-2): per-country teams,
 * properties, contracts and monthly contract value, plus a count of teams with no country set.
 */
@SkipTestCoverage
public record GeoResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    List<CountryStats> countries,
    long teamsWithoutCountry) {}
