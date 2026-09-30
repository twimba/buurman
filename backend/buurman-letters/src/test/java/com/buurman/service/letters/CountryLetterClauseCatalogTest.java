package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CountryLetterClauseCatalog")
class CountryLetterClauseCatalogTest {

  @Test
  @DisplayName(
      "DE/rent-increase-letter resolves the §558 section before the comparison-method section, in"
          + " order")
  void deRentIncreaseLetterResolvesTwoOrderedClauses() {
    List<LetterClauseKey> clauses =
        CountryLetterClauseCatalog.resolve("rent-increase-letter", "DE");

    assertThat(clauses)
        .containsExactly(
            new LetterClauseKey("legal.DE.section558.title", "legal.DE.section558.body"),
            new LetterClauseKey(
                "legal.DE.comparisonMethod.title", "legal.DE.comparisonMethod.body"));
  }

  @Test
  @DisplayName("NL/rent-increase-letter resolves exactly one clause")
  void nlRentIncreaseLetterResolvesOneClause() {
    List<LetterClauseKey> clauses =
        CountryLetterClauseCatalog.resolve("rent-increase-letter", "NL");

    assertThat(clauses)
        .containsExactly(new LetterClauseKey("legal.NL.basis.title", "legal.NL.basis.body"));
  }

  @Test
  @DisplayName("an unconfigured document type/country combination resolves to no entries")
  void unconfiguredCombinationResolvesEmpty() {
    assertThat(CountryLetterClauseCatalog.resolve("rent-increase-letter", "FR")).isEmpty();
    assertThat(CountryLetterClauseCatalog.resolve("extension-addendum", "DE")).isEmpty();
    assertThat(CountryLetterClauseCatalog.resolve("deposit-statement", "DE")).isEmpty();
  }

  @Test
  @DisplayName("case is normalized: a lowercase country code still resolves")
  void countryCodeIsCaseNormalized() {
    assertThat(CountryLetterClauseCatalog.resolve("rent-increase-letter", "de")).hasSize(2);
  }
}
