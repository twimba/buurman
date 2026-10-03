package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.buurman.domain.LeaseKind;

class LeaseDocumentLocatorTest {

  private final LeaseDocumentLocator locator = new LeaseDocumentLocator();

  @Test
  @DisplayName("falls back requested -> national -> English, and flags non-authoritative")
  void fallbackChain() {
    assertThat(locator.locate("NL", LeaseKind.RESIDENTIAL, "fr").orElseThrow().languageUsed())
        .isEqualTo("nl");
    assertThat(locator.locate("NL", LeaseKind.RESIDENTIAL, "en").orElseThrow().authoritative())
        .isFalse();
    assertThat(locator.locate("NL", LeaseKind.RESIDENTIAL, "nl").orElseThrow().authoritative())
        .isTrue();
    assertThat(locator.locate("BE", LeaseKind.RESIDENTIAL, "nl")).isEmpty();
  }

  @Test
  @DisplayName("template path is relative to templates/documents without extension")
  void templatePath() {
    assertThat(locator.locate("NL", LeaseKind.RESIDENTIAL, "nl").orElseThrow().templatePath())
        .isEqualTo("lease-agreement/NL/residential/nl");
  }

  @Test
  @DisplayName("unsupported kind or unknown country yields empty")
  void emptyWhenNoDocument() {
    assertThat(locator.locate("NL", LeaseKind.COMMERCIAL, "nl")).isEmpty();
    assertThat(locator.locate("XX", LeaseKind.RESIDENTIAL, "en")).isEmpty();
    assertThat(locator.locate(null, LeaseKind.RESIDENTIAL, "en")).isEmpty();
  }

  @Test
  @DisplayName("English fallback is used when only English exists, non-authoritative for NL")
  void englishFallbackFromUnsupportedLanguage() {
    var doc = locator.locate("ZZ", LeaseKind.RESIDENTIAL, "de").orElseThrow();
    assertThat(doc.languageUsed()).isEqualTo("en");
    assertThat(doc.authoritative()).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"../../etc/passwd", "nl/../x", "NL", "  ", "xx", "nl\u0000"})
  @DisplayName("hostile or unsupported requested language never reaches the path")
  void requestedLangIsAllowlisted(String lang) {
    var doc = locator.locate("NL", LeaseKind.RESIDENTIAL, lang).orElseThrow();
    assertThat(doc.templatePath()).isEqualTo("lease-agreement/NL/residential/nl");
    assertThat(doc.templatePath()).doesNotContain("..");
    assertThat(doc.templatePath().split("/")).hasSize(4);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"../", "nl", "NLD", " ", "N", "N/", "../NL", "Nl"})
  @DisplayName("country must be exactly two upper-case letters")
  void countryIsValidated(String country) {
    assertThat(locator.locate(country, LeaseKind.RESIDENTIAL, "nl")).isEmpty();
  }
}
