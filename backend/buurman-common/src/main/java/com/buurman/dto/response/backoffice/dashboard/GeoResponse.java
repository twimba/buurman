package com.buurman.dto.response.backoffice.dashboard;

import java.util.List;
import java.util.Optional;

import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.util.SkipTestCoverage;

/**
 * Geographic distribution. Teams grouped by their default country, properties grouped by their
 * country, plus a count of teams that have no country set. (ISO 3166-1 alpha-2 codes.)
 */
@SkipTestCoverage
public record GeoResponse(
    PanelStatus status,
    Optional<String> previewCta,
    Optional<String> docsLink,
    List<GeoCountry> teamCountries,
    List<GeoCountry> propertyCountries,
    long teamsWithoutCountry) {

  @SkipTestCoverage
  public record GeoCountry(String code, long count) {}
}
