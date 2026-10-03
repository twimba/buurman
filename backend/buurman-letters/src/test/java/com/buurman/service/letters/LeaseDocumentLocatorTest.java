package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.buurman.domain.LeaseKind;

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
}
