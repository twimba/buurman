package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * Guards the per-booklet message-source decision in {@code DocumentTemplateConfig}: the three
 * *-booklet bundles share generic keys (cover.title, value.*), so merging them into one source
 * resolves those keys to whichever basename is first — the bug that made the contract/contact
 * booklets render the property title. Each legacy exporter must bind to its own scoped source.
 */
@DisplayName("booklet message-source scoping")
class BookletMessageSourceScopingTest {

  private static ReloadableResourceBundleMessageSource source(String... basenames) {
    ReloadableResourceBundleMessageSource s = new ReloadableResourceBundleMessageSource();
    s.setBasenames(basenames);
    s.setDefaultEncoding("UTF-8");
    s.setUseCodeAsDefaultMessage(true);
    return s;
  }

  @Test
  @DisplayName("merging bundles makes a shared key resolve to the first basename (the bug)")
  void mergedSourceCollides() {
    ReloadableResourceBundleMessageSource merged =
        source(
            "classpath:messages/test-property-booklet", "classpath:messages/test-contact-booklet");
    assertThat(merged.getMessage("cover.title", null, Locale.ENGLISH)).isEqualTo("PROPERTY REPORT");
  }

  @Test
  @DisplayName("a source scoped to one bundle resolves that bundle's title (the fix)")
  void scopedSourceIsCorrect() {
    assertThat(
            source("classpath:messages/test-contact-booklet")
                .getMessage("cover.title", null, Locale.ENGLISH))
        .isEqualTo("CONTACT BOOKLET");
  }
}
