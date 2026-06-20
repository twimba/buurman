package com.buurman.domain.regulation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Canonical, version-controlled snapshot of the full rent-regulation reference dataset shipped with
 * the application (see {@code buurman-backoffice/src/main/resources/rent-regulations/}).
 *
 * <p>This file is the single source of truth for rent-regulation reference data going forward.
 * Rather than encoding every correction as a new Flyway migration, maintainers edit the JSON file
 * (or regenerate it via the backoffice Export action) and reload it via the backoffice Reload
 * action, which wipes and re-seeds the reference tables from this dataset.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RentRegulationCatalog(
    String version, String generatedAt, String description, List<CatalogCountry> countries) {

  public int countryCount() {
    return countries == null ? 0 : countries.size();
  }

  public int regionCount() {
    if (countries == null) {
      return 0;
    }
    return countries.stream()
        .mapToInt(c -> c.regions() == null ? 0 : c.regions().size())
        .sum();
  }

  public int ruleCount() {
    if (countries == null) {
      return 0;
    }
    return countries.stream().mapToInt(c -> c.rules() == null ? 0 : c.rules().size()).sum();
  }
}
