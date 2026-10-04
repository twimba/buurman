package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.buurman.domain.LeaseKind;
import com.buurman.util.DocumentLanguages;

class LeaseDocumentLocatorTest {

  private final LeaseDocumentLocator locator =
      new LeaseDocumentLocator(Map.of("ZZ", List.of("nl")));

  @Test
  @DisplayName("falls back requested -> national -> English, and flags non-authoritative")
  void fallbackChain() {
    assertThat(locator.locate("ZZ", LeaseKind.RESIDENTIAL, "fr").orElseThrow().languageUsed())
        .isEqualTo("nl");
    assertThat(locator.locate("ZZ", LeaseKind.RESIDENTIAL, "en").orElseThrow().authoritative())
        .isFalse();
    assertThat(locator.locate("ZZ", LeaseKind.RESIDENTIAL, "nl").orElseThrow().authoritative())
        .isTrue();
    assertThat(locator.locate("YY", LeaseKind.RESIDENTIAL, "nl")).isEmpty();
  }

  @Test
  @DisplayName(
      "a country without a national language flags its documents as having no national version")
  void noNationalVersionFlag() {
    assertThat(locator.locate("ZZ", LeaseKind.RESIDENTIAL, "en").orElseThrow().noNationalVersion())
        .as("ZZ has a national language (nl)")
        .isFalse();
    var none =
        new LeaseDocumentLocator(Map.of("ZZ", List.of())).locate("ZZ", LeaseKind.RESIDENTIAL, "en");
    assertThat(none.orElseThrow().noNationalVersion()).isTrue();
    assertThat(none.orElseThrow().authoritative()).isFalse();
    assertThat(new LeaseDocumentLocator().locate("CZ", LeaseKind.RESIDENTIAL, "en").orElseThrow())
        .extracting(LeaseDocumentLocator.LeaseDocument::noNationalVersion)
        .isEqualTo(true);
    assertThat(new LeaseDocumentLocator().locate("DE", LeaseKind.RESIDENTIAL, "en").orElseThrow())
        .extracting(LeaseDocumentLocator.LeaseDocument::noNationalVersion)
        .isEqualTo(false);
  }

  @Test
  @DisplayName("template path is relative to templates/documents without extension")
  void templatePath() {
    assertThat(locator.locate("ZZ", LeaseKind.RESIDENTIAL, "nl").orElseThrow().templatePath())
        .isEqualTo("lease-agreement/ZZ/residential/nl");
  }

  @Test
  @DisplayName("unsupported kind or unknown country yields empty")
  void emptyWhenNoDocument() {
    assertThat(locator.locate("ZZ", LeaseKind.COMMERCIAL, "nl")).isEmpty();
    assertThat(locator.locate("XX", LeaseKind.RESIDENTIAL, "en")).isEmpty();
    assertThat(locator.locate(null, LeaseKind.RESIDENTIAL, "en")).isEmpty();
  }

  @Test
  @DisplayName("English fallback is used when only English exists, non-authoritative for NL")
  void englishFallbackFromUnsupportedLanguage() {
    var doc =
        new LeaseDocumentLocator(Map.of()).locate("ZZ", LeaseKind.RESIDENTIAL, "de").orElseThrow();
    assertThat(doc.languageUsed()).isEqualTo("en");
    assertThat(doc.authoritative()).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"../../etc/passwd", "nl/../x", "ZZ", "  ", "xx", "nl\u0000"})
  @DisplayName("hostile or unsupported requested language never reaches the path")
  void requestedLangIsAllowlisted(String lang) {
    var doc = locator.locate("ZZ", LeaseKind.RESIDENTIAL, lang).orElseThrow();
    assertThat(doc.templatePath()).isEqualTo("lease-agreement/ZZ/residential/nl");
    assertThat(doc.templatePath()).doesNotContain("..");
    assertThat(doc.templatePath().split("/")).hasSize(4);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"../", "zz", "ZZZ", " ", "Z", "Z/", "../ZZ", "Zz"})
  @DisplayName("country must be exactly two upper-case letters")
  void countryIsValidated(String country) {
    assertThat(locator.locate(country, LeaseKind.RESIDENTIAL, "nl")).isEmpty();
  }

  @Test
  @DisplayName("a furnished unit uses the residential document until a furnished one exists")
  void furnishedFallsBackToResidentialDocument() {
    var doc = locator.locate("ZZ", LeaseKind.RESIDENTIAL_FURNISHED, "nl").orElseThrow();
    assertThat(doc.templatePath()).isEqualTo("lease-agreement/ZZ/residential/nl");
  }

  @Test
  @DisplayName("a furnished document wins over the residential one, even in another language")
  void furnishedDocumentTakesPrecedence() {
    var zy = new LeaseDocumentLocator(Map.of("ZY", List.of("nl")));
    assertThat(zy.locate("ZY", LeaseKind.RESIDENTIAL_FURNISHED, "nl").orElseThrow().templatePath())
        .isEqualTo("lease-agreement/ZY/residential-furnished/en");
    assertThat(zy.locate("ZY", LeaseKind.RESIDENTIAL, "nl").orElseThrow().templatePath())
        .isEqualTo("lease-agreement/ZY/residential/nl");
  }

  /** Country to its national languages, in preference order (D1 of the all-countries design). */
  private static Stream<Arguments> nationalLanguageTable() {
    return Stream.of(
        Arguments.of("AT", List.of("de")),
        Arguments.of("BE", List.of("nl", "fr")),
        Arguments.of("CA", List.of("en", "fr")),
        Arguments.of("CH", List.of("de", "fr", "it")),
        Arguments.of("CZ", List.of()),
        Arguments.of("DE", List.of("de")),
        Arguments.of("DK", List.of("da")),
        Arguments.of("ES", List.of("es")),
        Arguments.of("FI", List.of("fi", "sv")),
        Arguments.of("FR", List.of("fr")),
        Arguments.of("GB", List.of("en")),
        Arguments.of("GR", List.of("el")),
        Arguments.of("IE", List.of("en")),
        Arguments.of("IT", List.of("it")),
        Arguments.of("LU", List.of("fr", "de")),
        Arguments.of("NL", List.of("nl")),
        Arguments.of("NO", List.of("nb")),
        Arguments.of("PL", List.of("pl")),
        Arguments.of("PT", List.of("pt")),
        Arguments.of("SE", List.of("sv")),
        Arguments.of("US", List.of("en")));
  }

  private final LeaseDocumentLocator defaults = new LeaseDocumentLocator();

  @ParameterizedTest(name = "{0}")
  @MethodSource("nationalLanguageTable")
  @DisplayName("the default table lists the national languages of every catalog country in order")
  void nationalLanguagesPerCountry(String country, List<String> expected) {
    assertThat(defaults.nationalLanguages(country)).containsExactlyElementsOf(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("nationalLanguageTable")
  @DisplayName("fallback order is requested -> national languages in listed order -> English")
  void fallbackOrderPerCountry(String country, List<String> national) {
    // an unsupported request contributes nothing
    List<String> expected = new ArrayList<>(national);
    if (!expected.contains("en")) {
      expected.add("en");
    }
    assertThat(defaults.languageChain(country, "xx")).containsExactlyElementsOf(expected);
    assertThat(defaults.languageChain(country, null)).containsExactlyElementsOf(expected);

    // a supported request that is not national goes first, nothing is duplicated
    String requested = national.contains("pl") ? "sv" : "pl";
    List<String> withRequest = new ArrayList<>();
    withRequest.add(requested);
    withRequest.addAll(expected);
    assertThat(defaults.languageChain(country, requested))
        .containsExactlyElementsOf(withRequest)
        .doesNotHaveDuplicates();

    // requesting a national language moves it to the front; the rest keeps its listed order
    for (String lang : national) {
      List<String> rest = new ArrayList<>(expected);
      rest.remove(lang);
      List<String> chain = new ArrayList<>();
      chain.add(lang);
      chain.addAll(rest);
      assertThat(defaults.languageChain(country, lang)).containsExactlyElementsOf(chain);
    }
  }

  @Test
  @DisplayName("a country without a national language (CZ) falls back to the request, then English")
  void czechiaHasNoNationalLanguage() {
    assertThat(defaults.nationalLanguages("CZ")).isEmpty();
    assertThat(defaults.languageChain("CZ", "de")).containsExactly("de", "en");
    assertThat(defaults.languageChain("CZ", "en")).containsExactly("en");
  }

  @Test
  @DisplayName("a country outside the table has no national language")
  void unknownCountryHasNoNationalLanguage() {
    assertThat(defaults.nationalLanguages("JP")).isEmpty();
    assertThat(defaults.languageChain("JP", "fr")).containsExactly("fr", "en");
  }

  @Test
  @DisplayName("the authoritative flag is set exactly for a national language of the country")
  void authoritativeFlagFollowsNationalLanguages() {
    var be = new LeaseDocumentLocator(Map.of("ZB", List.of("nl", "fr")));
    assertThat(be.nationalLanguages("ZB")).contains("nl", "fr").doesNotContain("en");
    // ZZ fixtures only exist in nl/en; the flag of a located document follows the table
    assertThat(locator.locate("ZZ", LeaseKind.RESIDENTIAL, "nl").orElseThrow().authoritative())
        .isTrue();
    assertThat(
            new LeaseDocumentLocator(Map.of("ZZ", List.of("fr")))
                .locate("ZZ", LeaseKind.RESIDENTIAL, "nl")
                .orElseThrow()
                .authoritative())
        .as("nl is not national for this fake table")
        .isFalse();
    assertThat(
            new LeaseDocumentLocator(Map.of("ZZ", List.of("en")))
                .locate("ZZ", LeaseKind.RESIDENTIAL, "en")
                .orElseThrow()
                .authoritative())
        .as("English is authoritative where it is the first national language (GB, IE, US)")
        .isTrue();
    assertThat(
            new LeaseDocumentLocator(Map.of("ZZ", List.of("nl", "en")))
                .locate("ZZ", LeaseKind.RESIDENTIAL, "en")
                .orElseThrow()
                .authoritative())
        .as("a second national language is a translation, not the authoritative document")
        .isFalse();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("nationalLanguageTable")
  @DisplayName("only the FIRST listed national language is authoritative; CZ has none")
  void onlyFirstNationalLanguageIsAuthoritative(String country, List<String> national) {
    for (String lang : DocumentLanguages.ORDERED) {
      boolean expected = !national.isEmpty() && national.get(0).equals(lang);
      assertThat(defaults.isAuthoritative(country, lang))
          .as("%s/%s authoritative", country, lang)
          .isEqualTo(expected);
    }
  }

  @Test
  @DisplayName(
      "registry: a located document is authoritative exactly for the entry's authoritative"
          + " language")
  void locatedAuthoritativeFlagMatchesRegistry() {
    for (LeaseDocumentRegistry.Entry entry : LeaseDocumentRegistry.ENTRIES) {
      boolean nationalCountry = !defaults.nationalLanguages(entry.countryCode()).isEmpty();
      for (String lang : entry.enforcedLanguages()) {
        var doc = defaults.locate(entry.countryCode(), entry.kind(), lang).orElseThrow();
        assertThat(doc.languageUsed()).isEqualTo(lang);
        // CZ and other countries without a national language: even the registry's authoritative
        // (English) document is flagged non-authoritative at runtime and shows the courtesy notice
        assertThat(doc.authoritative())
            .as("%s/%s", entry.key(), lang)
            .isEqualTo(nationalCountry && lang.equals(entry.authoritativeLanguage()));
        // and that document gets the no-national-version notice instead of the courtesy one
        assertThat(doc.noNationalVersion())
            .as("%s/%s", entry.key(), lang)
            .isEqualTo(!nationalCountry);
      }
    }
  }
}
