package com.buurman.util;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * ISO 639-1 codes a generated document (booklet, summary card, letter, email) can be rendered in.
 */
public final class DocumentLanguages {

  /**
   * The supported languages in a stable, human-meaningful order. {@code SUPPORTED} is a {@code Set}
   * whose iteration order is unspecified, which makes parameterized test names and report output
   * shuffle between runs. Anything that iterates the languages should use this.
   */
  public static final List<String> ORDERED =
      List.of("en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb");

  public static final Set<String> SUPPORTED = Set.copyOf(ORDERED);

  public static final List<Locale> LOCALES = ORDERED.stream().map(Locale::forLanguageTag).toList();

  public static final String DEFAULT = "en";

  private DocumentLanguages() {}

  public static boolean isSupported(String lang) {
    return SUPPORTED.contains(lang);
  }

  /** First supported language of the list, or English. */
  public static String firstSupportedOrDefault(List<String> languages) {
    return languages.stream().filter(SUPPORTED::contains).findFirst().orElse(DEFAULT);
  }
}
