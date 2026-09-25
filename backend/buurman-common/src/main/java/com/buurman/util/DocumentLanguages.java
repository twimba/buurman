package com.buurman.util;

import java.util.List;
import java.util.Set;

/**
 * ISO 639-1 codes a generated document (booklet, summary card, letter, email) can be rendered in.
 */
public final class DocumentLanguages {

  public static final Set<String> SUPPORTED =
      Set.of("en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb");

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
