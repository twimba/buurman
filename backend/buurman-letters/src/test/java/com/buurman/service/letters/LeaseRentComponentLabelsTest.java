package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.MessageSource;

import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.RentComponentType;
import com.buurman.util.DocumentLanguages;

@DisplayName("lease rent component labels")
class LeaseRentComponentLabelsTest {

  private static final MessageSource MESSAGES =
      DocumentTemplateSupport.messageSource(false, "classpath:messages/document-lease-agreement");

  private static String label(RentComponentType type, String lang) {
    return MESSAGES.getMessage(
        "lease.rentComponent." + type.name(), null, Locale.forLanguageTag(lang));
  }

  @ParameterizedTest
  @EnumSource(RentComponentType.class)
  @DisplayName("every rent component type has a label key in the base bundle and every language")
  void everyTypeHasAKeyInEveryLanguage(RentComponentType type) {
    for (String lang : DocumentLanguages.ORDERED) {
      assertThat(label(type, lang))
          .as("%s in %s", type, lang)
          .isNotBlank()
          .doesNotStartWith("lease.rentComponent.");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"nl", "de", "fr", "es", "it", "pt", "sv", "da", "nb", "fi", "pl", "el"})
  @DisplayName("translated bundles do not just repeat the English display name")
  void translatedBundlesAreLocalized(String lang) {
    assertThat(label(RentComponentType.BASE_RENT, lang))
        .isNotEqualTo(RentComponentType.BASE_RENT.getDisplayName());
  }

  @Test
  @DisplayName("Dutch and German label the service costs in their own language")
  void dutchAndGerman() {
    assertThat(label(RentComponentType.BASE_RENT, "nl")).isEqualTo("Kale huur");
    assertThat(label(RentComponentType.SERVICE_COSTS, "nl")).isEqualTo("Servicekosten");
    assertThat(label(RentComponentType.BASE_RENT, "de")).isEqualTo("Grundmiete");
    assertThat(label(RentComponentType.UTILITIES_ADVANCE, "de")).isEqualTo("Nebenkostenvorschuss");
  }
}
