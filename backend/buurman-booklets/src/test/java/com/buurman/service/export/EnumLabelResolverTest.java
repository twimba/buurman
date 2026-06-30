package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.buurman.domain.Property.PropertyStatus;

@DisplayName("EnumLabelResolver")
class EnumLabelResolverTest {

  private static final Locale NL = Locale.forLanguageTag("nl");

  private EnumLabelResolver resolver;

  @BeforeEach
  void setUp() {
    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();
    messageSource.setBasenames("classpath:messages/test-enum-labels");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setUseCodeAsDefaultMessage(true);
    resolver = new EnumLabelResolver(messageSource);
  }

  @Test
  @DisplayName("resolves a known enum value from the bundle, namespace derived from class name")
  void resolvesKnownValue() {
    assertThat(resolver.label(PropertyStatus.VACANT, Locale.ENGLISH)).isEqualTo("Vacant");
  }

  @Test
  @DisplayName("resolves the locale-specific translation when present")
  void resolvesLocale() {
    assertThat(resolver.label(PropertyStatus.VACANT, NL)).isEqualTo("Leegstaand");
  }

  @Test
  @DisplayName("falls back to a Title-Cased label for a missing key (never a raw SNAKE_CASE code)")
  void fallsBackToTitleCase() {
    assertThat(resolver.label(PropertyStatus.UNDER_RENOVATION, Locale.ENGLISH))
        .isEqualTo("Under Renovation");
  }

  @Test
  @DisplayName("resolves by explicit namespace + raw code")
  void resolvesByNamespace() {
    assertThat(resolver.label("paymentMethod", "IDEAL_WERO", Locale.ENGLISH))
        .isEqualTo("iDEAL / Wero");
  }

  @Test
  @DisplayName("returns empty string for null enum or blank code")
  void handlesNull() {
    assertThat(resolver.label((Enum<?>) null, Locale.ENGLISH)).isEmpty();
    assertThat(resolver.label("paymentMethod", null, Locale.ENGLISH)).isEmpty();
    assertThat(resolver.label("paymentMethod", "  ", Locale.ENGLISH)).isEmpty();
  }
}
