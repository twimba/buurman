package com.buurman.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.buurman.exception.BadRequestException;

@DisplayName("DocumentLocale")
class DocumentLocaleTest {

  @ParameterizedTest(name = "resolves ''{0}'' to a valid locale")
  @ValueSource(
      strings = {"en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb"})
  void resolvesAllSupportedLanguages(String lang) {
    assertThat(DocumentLocale.resolve(lang).getLanguage()).isEqualTo(lang);
    assertThat(DocumentLocale.resolveOrEnglish(lang).getLanguage()).isEqualTo(lang);
  }

  @ParameterizedTest(name = "rejects unsupported language ''{0}''")
  @ValueSource(strings = {"xx", "ja", "zh", "", "en-US", "../../etc"})
  void rejectsUnsupportedLanguages(String lang) {
    assertThatThrownBy(() -> DocumentLocale.resolve(lang))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Unsupported document language");
    assertThat(DocumentLocale.resolveOrEnglish(lang)).isEqualTo(Locale.ENGLISH);
  }
}
