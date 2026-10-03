package com.buurman.service.letters;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.buurman.document.DocumentTemplateSupport;
import com.buurman.domain.LeaseKind;
import com.buurman.util.DocumentLanguages;

/**
 * Finds the per-language clause document for a country and lease kind, falling back from the
 * requested language to the country's national language(s) and finally English.
 */
@Component
public class LeaseDocumentLocator {

  static final String BASE = "lease-agreement";

  /** The country and language segments are interpolated into a classpath path: allowlist both. */
  private static final Pattern COUNTRY_PATTERN = Pattern.compile("[A-Z]{2}");

  private static final Map<String, List<String>> NATIONAL_LANGUAGES =
      Map.of(
          "NL", List.of("nl"),
          "BE", List.of("nl", "fr"),
          "DE", List.of("de"),
          "ES", List.of("es"),
          "FR", List.of("fr"),
          "GB", List.of("en"),
          "PT", List.of("pt"));

  /**
   * @param templatePath template name relative to {@code templates/documents/}, without {@code
   *     .html}
   * @param languageUsed the language of the document actually found
   * @param authoritative whether {@code languageUsed} is a national language of the country (else
   *     the document is a courtesy translation)
   */
  public record LeaseDocument(String templatePath, String languageUsed, boolean authoritative) {}

  public Optional<LeaseDocument> locate(String countryCode, LeaseKind kind, String requestedLang) {
    if (kind == null || countryCode == null || !COUNTRY_PATTERN.matcher(countryCode).matches()) {
      return Optional.empty();
    }
    String country = countryCode;
    List<String> national = NATIONAL_LANGUAGES.getOrDefault(country, List.of());

    Set<String> chain = new LinkedHashSet<>();
    if (requestedLang != null && DocumentLanguages.isSupported(requestedLang)) {
      chain.add(requestedLang);
    }
    chain.addAll(national);
    chain.add("en");

    return new ArrayList<>(chain)
        .stream()
            .filter(lang -> exists(country, kind, lang))
            .findFirst()
            .map(
                lang ->
                    new LeaseDocument(path(country, kind, lang), lang, national.contains(lang)));
  }

  private static String path(String country, LeaseKind kind, String lang) {
    return BASE + "/" + country + "/" + kind.pathSegment() + "/" + lang;
  }

  private static boolean exists(String country, LeaseKind kind, String lang) {
    return new ClassPathResource(
            DocumentTemplateSupport.TEMPLATE_PREFIX + path(country, kind, lang) + ".html")
        .exists();
  }
}
