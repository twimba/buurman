package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Every supported locale of the summary-card bundle must define exactly the same keys as the
 * English base (no missing or extra keys) with non-blank values — so a card never falls back to
 * English chrome or renders a raw key. Guards the i18n completeness the cards depend on.
 */
@DisplayName("summary-card bundle i18n parity")
class SummaryCardBundleParityTest {

  private static final List<String> LOCALES =
      List.of("da", "de", "el", "es", "fi", "fr", "it", "nb", "nl", "pl", "pt", "sv");

  private static Properties load(String resource) throws Exception {
    Properties p = new Properties();
    try (InputStream in = SummaryCardBundleParityTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing bundle: " + resource);
      p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    return p;
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"da", "de", "el", "es", "fi", "fr", "it", "nb", "nl", "pl", "pt", "sv"})
  @DisplayName("locale has identical key set + non-blank values vs English base")
  void localeMatchesBase(String locale) throws Exception {
    Set<String> base = load("/messages/document-summary-card.properties").stringPropertyNames();
    Properties loc = load("/messages/document-summary-card_" + locale + ".properties");

    assertThat(loc.stringPropertyNames())
        .as("key set for %s", locale)
        .containsExactlyInAnyOrderElementsOf(base);
    base.forEach(
        k -> assertThat(loc.getProperty(k)).as("%s [%s]", k, locale).isNotNull().isNotBlank());
  }

  @org.junit.jupiter.api.Test
  @DisplayName("all 12 non-English locales are present")
  void allLocalesPresent() throws Exception {
    for (String l : LOCALES) {
      assertNotNull(
          SummaryCardBundleParityTest.class.getResourceAsStream(
              "/messages/document-summary-card_" + l + ".properties"),
          "missing locale " + l);
    }
  }
}
