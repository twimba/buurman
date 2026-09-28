package com.buurman.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DocumentLanguages")
class DocumentLanguagesTest {

  @Test
  @DisplayName("ORDERED lists the thirteen supported languages in a stable order")
  void orderedIsStable() {
    assertThat(DocumentLanguages.ORDERED)
        .containsExactly(
            "en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb");
  }

  @Test
  @DisplayName("ORDERED and SUPPORTED describe the same set")
  void orderedAndSupportedAgree() {
    assertThat(DocumentLanguages.ORDERED)
        .containsExactlyInAnyOrderElementsOf(DocumentLanguages.SUPPORTED);
    assertThat(DocumentLanguages.ORDERED).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("LOCALES mirrors ORDERED position for position")
  void localesMirrorOrdered() {
    assertThat(DocumentLanguages.LOCALES).hasSameSizeAs(DocumentLanguages.ORDERED);
    for (int i = 0; i < DocumentLanguages.ORDERED.size(); i++) {
      assertThat(DocumentLanguages.LOCALES.get(i))
          .isEqualTo(Locale.forLanguageTag(DocumentLanguages.ORDERED.get(i)));
    }
  }

  @Test
  @DisplayName("English is the default and is supported")
  void englishIsDefault() {
    assertThat(DocumentLanguages.DEFAULT).isEqualTo("en");
    assertThat(DocumentLanguages.isSupported(DocumentLanguages.DEFAULT)).isTrue();
  }
}
