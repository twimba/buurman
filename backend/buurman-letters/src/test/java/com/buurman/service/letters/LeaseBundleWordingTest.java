package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSource;

import com.buurman.document.DocumentTemplateSupport;

@DisplayName("lease bundle wording shared by every document kind")
class LeaseBundleWordingTest {

  private static final MessageSource MESSAGES =
      DocumentTemplateSupport.messageSource(false, "classpath:messages/document-lease-agreement");

  private static String message(String key, String lang) {
    return MESSAGES.getMessage(key, new Object[] {"X-ADDRESS"}, Locale.forLanguageTag(lang));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"en", "nl", "de", "fr", "es", "it", "pt", "sv", "da", "nb", "fi", "pl", "el"})
  @DisplayName("lease.premises is neutral (no dwelling-only wording) and keeps the address")
  void premisesIsNeutral(String lang) {
    assertThat(message("lease.premises", lang)).contains("X-ADDRESS").doesNotContain("woonruimte");
    assertThat(message("lease.premises", "nl")).contains("het gehuurde gelegen aan");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"en", "nl", "de", "fr", "es", "it", "pt", "sv", "da", "nb", "fi", "pl", "el"})
  @DisplayName("the legacy disclaimer and the shell disclaimer are distinct and both present")
  void disclaimersAreDistinct(String lang) {
    assertThat(message("lease.disclaimer.legacy", lang))
        .isNotBlank()
        .doesNotStartWith("lease.disclaimer")
        .isNotEqualTo(message("lease.disclaimer", lang));
    assertThat(message("lease.disclaimer", lang)).doesNotStartWith("lease.disclaimer");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"en", "nl", "de", "fr", "es", "it", "pt", "sv", "da", "nb", "fi", "pl", "el"})
  @DisplayName("the no-national-version notice is translated and distinct from the courtesy notice")
  void noNationalVersionNoticeIsTranslated(String lang) {
    String notice = message("lease.notice.noNationalVersion", lang);
    assertThat(notice)
        .doesNotStartWith("lease.notice")
        .doesNotContain("!")
        .doesNotContain("\'")
        .isNotEqualTo(message("lease.notice.courtesy", lang));
    if (!lang.equals("en")) {
      assertThat(notice).isNotEqualTo(message("lease.notice.noNationalVersion", "en"));
    }
  }
}
