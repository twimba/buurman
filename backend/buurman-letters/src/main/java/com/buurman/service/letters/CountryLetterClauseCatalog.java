package com.buurman.service.letters;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Structural country/document-type variation for letter legal clauses. A combination with no
 * entry here falls back to the single legacy {@code legal.{COUNTRY}}-style clause resolution in
 * {@link LetterExporterHelper#resolveLegalClause}, so adding a country/document-type here is
 * purely additive — nothing existing changes shape unless explicitly listed.
 *
 * <p>A static config class, not a database table: this changes with template structure (the
 * {@code th:each} loop in {@code generic.html} already requires a deploy to change), so letting
 * it be edited without a deploy would let content and structure drift out of sync.
 */
final class CountryLetterClauseCatalog {

  private static final Map<String, Map<String, List<LetterClauseKey>>> CLAUSES =
      Map.of(
          "rent-increase-letter",
          Map.of(
              "NL",
              List.of(new LetterClauseKey("legal.NL.basis.title", "legal.NL.basis.body")),
              "DE",
              List.of(
                  new LetterClauseKey("legal.DE.section558.title", "legal.DE.section558.body"),
                  new LetterClauseKey(
                      "legal.DE.comparisonMethod.title", "legal.DE.comparisonMethod.body"))));

  private CountryLetterClauseCatalog() {}

  /** Ordered clause keys for the given document type and country, or empty if not configured. */
  static List<LetterClauseKey> resolve(String documentType, String countryCode) {
    return CLAUSES
        .getOrDefault(documentType, Map.of())
        .getOrDefault(countryCode.toUpperCase(Locale.ROOT), List.of());
  }
}
