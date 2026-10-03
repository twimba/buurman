package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.buurman.util.DocumentLanguages;

class LeaseMessageCatalogTest {

  private static final String NL_DEPOSIT_TITLE = "lease.nl.residential.deposit.title";

  private final LeaseMessageCatalog catalog = new LeaseMessageCatalog();

  @ParameterizedTest
  @ValueSource(strings = {"en", "nl", "sv"})
  @DisplayName("a real key is defined in the English base and in translated bundles")
  void realKeyDefinedInLanguage(String language) {
    assertThat(catalog.definedIn(language, NL_DEPOSIT_TITLE)).isTrue();
  }

  @Test
  @DisplayName("a key present only in the English base is not defined in other languages")
  void baseInheritanceIsNotDefined() {
    String baseOnly = "lease.test.only.in.base";
    LeaseMessageCatalog custom =
        new LeaseMessageCatalog("messages/lease-catalog-test", DocumentLanguages.ORDERED);

    assertThat(custom.definedIn("en", baseOnly)).isTrue();
    assertThat(custom.definedIn("nl", baseOnly)).isFalse();
    assertThat(custom.definedIn("nl", "lease.test.both")).isTrue();
    assertThat(custom.missingLanguages(baseOnly)).doesNotContain("en").contains("nl", "de", "nb");
    assertThat(custom.missingLanguages("lease.test.both"))
        .doesNotContain("en", "nl")
        .contains("de");
  }

  @Test
  @DisplayName("a blank value counts as not defined")
  void blankValueIsMissing() {
    LeaseMessageCatalog custom =
        new LeaseMessageCatalog("messages/lease-catalog-test", DocumentLanguages.ORDERED);
    assertThat(custom.definedIn("nl", "lease.test.blank")).isFalse();
  }

  @Test
  @DisplayName("a key missing everywhere is reported missing in all 13 languages")
  void missingEverywhere() {
    assertThat(catalog.missingLanguages("lease.no.such.key"))
        .containsExactlyElementsOf(DocumentLanguages.ORDERED);
  }

  @Test
  @DisplayName("a key present in every real bundle has no missing languages")
  void fullyTranslatedKey() {
    assertThat(catalog.missingLanguages(NL_DEPOSIT_TITLE)).isEmpty();
  }

  @Test
  @DisplayName("an unsupported language defines nothing")
  void unsupportedLanguage() {
    assertThat(catalog.definedIn("xx", NL_DEPOSIT_TITLE)).isFalse();
  }
}
