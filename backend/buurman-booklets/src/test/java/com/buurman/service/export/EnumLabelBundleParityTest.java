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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.buurman.domain.ContactAddress;
import com.buurman.domain.ContactTag;
import com.buurman.domain.ContactType;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Payment;
import com.buurman.domain.PaymentInstruction;
import com.buurman.domain.Property;
import com.buurman.domain.UnitStatus;

/**
 * Guards {@code document-enum-labels}: (1) every supported locale defines exactly the English
 * base's keys with non-blank values (no English fallthrough), and (2) every entity-facing enum
 * constant the booklets/cards render has a base key — so adding a new enum value without a label
 * fails CI instead of shipping a Title-Cased English fallback in all locales.
 */
@DisplayName("enum-labels bundle i18n parity + coverage")
class EnumLabelBundleParityTest {

  private static final List<String> LOCALES =
      List.of("da", "de", "el", "es", "fi", "fr", "it", "nb", "nl", "pl", "pt", "sv");

  /** The enum namespaces EnumLabelResolver resolves for booklets + summary cards. */
  private static final List<Class<? extends Enum<?>>> ENUMS =
      List.of(
          Property.PropertyType.class,
          Property.PropertyCategory.class,
          UnitStatus.class,
          Contract.ContractStatus.class,
          Contract.ContractType.class,
          Contract.PaymentFrequency.class,
          Payment.PaymentStatus.class,
          PaymentInstruction.PaymentMethod.class,
          ContractExtension.RentAdjustmentType.class,
          ContactType.class,
          ContractPartyRole.class,
          ContactTag.class,
          Contract.RenewalMode.class,
          ContactAddress.AddressType.class);

  private static Properties load(String resource) throws Exception {
    Properties p = new Properties();
    try (InputStream in = EnumLabelBundleParityTest.class.getResourceAsStream(resource)) {
      assertNotNull(in, "missing bundle: " + resource);
      p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    return p;
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"da", "de", "el", "es", "fi", "fr", "it", "nb", "nl", "pl", "pt", "sv"})
  @DisplayName("locale has identical key set + non-blank values vs English base")
  void localeMatchesBase(String locale) throws Exception {
    Set<String> base = load("/messages/document-enum-labels.properties").stringPropertyNames();
    Properties loc = load("/messages/document-enum-labels_" + locale + ".properties");

    assertThat(loc.stringPropertyNames())
        .as("key set for %s", locale)
        .containsExactlyInAnyOrderElementsOf(base);
    base.forEach(
        k -> assertThat(loc.getProperty(k)).as("%s [%s]", k, locale).isNotNull().isNotBlank());
  }

  @Test
  @DisplayName("every entity-facing enum constant has a base label key")
  void everyEnumConstantHasKey() throws Exception {
    Properties base = load("/messages/document-enum-labels.properties");
    for (Class<? extends Enum<?>> e : ENUMS) {
      String ns =
          Character.toLowerCase(e.getSimpleName().charAt(0)) + e.getSimpleName().substring(1);
      for (Enum<?> constant : e.getEnumConstants()) {
        String key = ns + "." + constant.name();
        assertThat(base.getProperty(key)).as("missing enum-label key: %s", key).isNotNull();
      }
    }
  }

  @Test
  @DisplayName("all 12 non-English locales are present")
  void allLocalesPresent() {
    for (String l : LOCALES) {
      assertNotNull(
          EnumLabelBundleParityTest.class.getResourceAsStream(
              "/messages/document-enum-labels_" + l + ".properties"),
          "missing locale " + l);
    }
  }
}
