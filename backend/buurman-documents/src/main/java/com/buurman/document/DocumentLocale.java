package com.buurman.document;

import java.util.Locale;
import java.util.Set;

import com.buurman.exception.BadRequestException;
import com.buurman.util.DocumentLanguages;

/** The languages a generated document (booklet, summary card or letter) can be rendered in. */
public final class DocumentLocale {

  public static final Set<String> SUPPORTED_LANGUAGES = DocumentLanguages.SUPPORTED;

  private DocumentLocale() {}

  /**
   * Resolves a Locale from an ISO 639-1 language code.
   *
   * @throws BadRequestException if the language is not supported
   */
  public static Locale resolve(String lang) {
    if (!SUPPORTED_LANGUAGES.contains(lang)) {
      throw new BadRequestException("Unsupported document language: " + lang);
    }
    return Locale.forLanguageTag(lang);
  }

  /** Lenient variant: unsupported codes fall back to English instead of failing. */
  public static Locale resolveOrEnglish(String lang) {
    return SUPPORTED_LANGUAGES.contains(lang) ? Locale.forLanguageTag(lang) : Locale.ENGLISH;
  }
}
