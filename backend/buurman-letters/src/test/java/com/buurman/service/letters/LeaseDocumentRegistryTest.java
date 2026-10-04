package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.LeaseKind;
import com.buurman.service.letters.LeaseDocumentRegistry.ClauseSpec;
import com.buurman.service.letters.LeaseDocumentRegistry.Entry;
import com.buurman.util.DocumentLanguages;

@DisplayName("lease document registry")
class LeaseDocumentRegistryTest {

  private static final List<ClauseSpec> CLAUSES = List.of(new ClauseSpec("rent", true, false, 1));

  @Test
  @DisplayName("(country, kind) pairs are unique")
  void uniquePairs() {
    assertThat(LeaseDocumentRegistry.ENTRIES.stream().map(Entry::dbKey).toList())
        .doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("NL residential: Dutch authoritative, all 13 languages enforced")
  void nlResidentialEntry() {
    Entry nl = LeaseDocumentRegistry.find("NL", LeaseKind.RESIDENTIAL).orElseThrow();
    assertThat(nl.authoritativeLanguage()).isEqualTo("nl");
    assertThat(nl.enforcedLanguages()).containsExactlyElementsOf(DocumentLanguages.ORDERED);
    assertThat(nl.translations()).hasSize(12).doesNotContain("nl");
    assertThat(nl.clauseKeys()).hasSize(16);
    assertThat(nl.key()).isEqualTo("NL/residential");
    assertThat(nl.dbKey()).isEqualTo("NL/RESIDENTIAL");
    assertThat(nl.i18nPrefix("rent")).isEqualTo("lease.nl.residential.rent");
    assertThat(nl.foreignMarkerPattern()).isPresent();
  }

  @Test
  @DisplayName("an entry without foreign markers has no marker pattern")
  void emptyMarkers() {
    Entry entry = new Entry("ZZ", LeaseKind.COMMERCIAL, "en", List.of("en"), CLAUSES, List.of());
    assertThat(entry.foreignMarkerPattern()).isEmpty();
    assertThat(entry.key()).isEqualTo("ZZ/commercial");
    assertThat(entry.dbKey()).isEqualTo("ZZ/COMMERCIAL");
    assertThat(entry.documentPath("en")).isEqualTo("lease-agreement/ZZ/commercial/en");
    assertThat(entry.translations()).isEmpty();
  }

  @Test
  @DisplayName("an invalid entry is rejected at construction")
  void invalidEntriesRejected() {
    assertThatThrownBy(
            () -> new Entry("zz", LeaseKind.RESIDENTIAL, "en", List.of("en"), CLAUSES, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new Entry("ZZ", LeaseKind.LEGACY, "en", List.of("en"), CLAUSES, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new Entry("ZZ", LeaseKind.RESIDENTIAL, "de", List.of("en"), CLAUSES, List.of()))
        .as("authoritative must be enforced")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new Entry("ZZ", LeaseKind.RESIDENTIAL, "xx", List.of("xx"), CLAUSES, List.of()))
        .as("unsupported language")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new Entry(
                    "ZZ", LeaseKind.RESIDENTIAL, "en", List.of("en", "en"), CLAUSES, List.of()))
        .as("duplicate language")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new Entry("ZZ", LeaseKind.RESIDENTIAL, "en", List.of("en"), List.of(), List.of()))
        .as("no clauses")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new Entry(
                    "ZZ",
                    LeaseKind.RESIDENTIAL,
                    "en",
                    List.of("en"),
                    List.of(
                        new ClauseSpec("a", true, false, 1), new ClauseSpec("a", false, false, 2)),
                    List.of()))
        .as("repeated clause key")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new Entry(
                    "ZZ",
                    LeaseKind.RESIDENTIAL,
                    "en",
                    List.of("en"),
                    List.of(new ClauseSpec("Bad Key", true, false, 1)),
                    List.of()))
        .as("unsafe clause key")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("every number-word rule set exists for the authoritative language of each entry")
  void authoritativeLanguageIsLintable() {
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      // throws IllegalArgumentException for a language without rules
      assertThat(
              LeaseNumberWords.violations(
                  "<div th:fragment=\"clause-a\"><p>x</p></div>", entry.authoritativeLanguage()))
          .isEmpty();
    }
  }

  @Test
  @DisplayName("the authoritative language is the first national language of the country")
  void authoritativeLanguageIsFirstNational() {
    LeaseDocumentLocator locator = new LeaseDocumentLocator();
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      List<String> national = locator.nationalLanguages(entry.countryCode());
      if (!national.isEmpty()) {
        assertThat(entry.authoritativeLanguage())
            .as("%s authoritative language", entry.key())
            .isEqualTo(national.get(0));
      }
    }
  }
}
