package com.buurman.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import com.buurman.util.DocumentLanguages;

@DisplayName("I18nConfig")
class I18nConfigTest {

  @Test
  @DisplayName("the locale resolver supports exactly the canonical languages")
  void resolverMatchesCanonicalList() {
    AcceptHeaderLocaleResolver resolver =
        (AcceptHeaderLocaleResolver) new I18nConfig().localeResolver();

    assertThat(resolver.getSupportedLocales()).containsExactlyElementsOf(DocumentLanguages.LOCALES);
  }

  @Test
  @DisplayName("a request asking for no supported language falls back to English")
  void unsupportedRequestFallsBackToEnglish() {
    AcceptHeaderLocaleResolver resolver =
        (AcceptHeaderLocaleResolver) new I18nConfig().localeResolver();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Accept-Language", "ja-JP");

    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.ENGLISH);
  }
}
