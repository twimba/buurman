package com.buurman.service.letters;

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
 * requested language to the country's national language(s) and finally English. The kind takes
 * precedence over the language: {@link LeaseKind#fallbackChain()} is walked outermost.
 */
@Component
public class LeaseDocumentLocator {

  static final String BASE = "lease-agreement";

  /** The country and language segments are interpolated into a classpath path: allowlist both. */
  private static final Pattern COUNTRY_PATTERN = Pattern.compile("[A-Z]{2}");

  /**
   * National languages per country in preference order; the order is meaningful: the FIRST language
   * is the authoritative one, the others are translations. Czechia has none among the supported
   * document languages: English is the only fallback there.
   */
  private static final Map<String, List<String>> DEFAULT_NATIONAL_LANGUAGES =
      Map.ofEntries(
          Map.entry("AT", List.of("de")),
          Map.entry("BE", List.of("nl", "fr")),
          Map.entry("CA", List.of("en", "fr")),
          Map.entry("CH", List.of("de", "fr", "it")),
          Map.entry("CZ", List.of()),
          Map.entry("DE", List.of("de")),
          Map.entry("DK", List.of("da")),
          Map.entry("ES", List.of("es")),
          Map.entry("FI", List.of("fi", "sv")),
          Map.entry("FR", List.of("fr")),
          Map.entry("GB", List.of("en")),
          Map.entry("GR", List.of("el")),
          Map.entry("IE", List.of("en")),
          Map.entry("IT", List.of("it")),
          Map.entry("LU", List.of("fr", "de")),
          Map.entry("NL", List.of("nl")),
          Map.entry("NO", List.of("nb")),
          Map.entry("PL", List.of("pl")),
          Map.entry("PT", List.of("pt")),
          Map.entry("SE", List.of("sv")),
          Map.entry("US", List.of("en")));

  private final Map<String, List<String>> nationalLanguages;

  public LeaseDocumentLocator() {
    this(DEFAULT_NATIONAL_LANGUAGES);
  }

  /** Test seam: lets tests register a fake country so they never shadow real documents. */
  LeaseDocumentLocator(Map<String, List<String>> nationalLanguages) {
    this.nationalLanguages = nationalLanguages;
  }

  /**
   * @param templatePath template name relative to {@code templates/documents/}, without {@code
   *     .html}
   * @param languageUsed the language of the document actually found
   * @param authoritative whether {@code languageUsed} is the first national language of the country
   *     (else the document is a courtesy translation)
   * @param noNationalVersion whether the country has no national document language at all (CZ): the
   *     document then shows the no-national-version notice instead of the courtesy notice, which
   *     would point to a national version that does not exist
   */
  public record LeaseDocument(
      String templatePath, String languageUsed, boolean authoritative, boolean noNationalVersion) {

    /** A document of a country that has a national document language. */
    public LeaseDocument(String templatePath, String languageUsed, boolean authoritative) {
      this(templatePath, languageUsed, authoritative, false);
    }
  }

  public Optional<LeaseDocument> locate(String countryCode, LeaseKind kind, String requestedLang) {
    if (kind == null || countryCode == null || !COUNTRY_PATTERN.matcher(countryCode).matches()) {
      return Optional.empty();
    }
    List<String> chain = languageChain(countryCode, requestedLang);

    return kind.fallbackChain().stream()
        .flatMap(
            k ->
                chain.stream()
                    .filter(lang -> exists(countryCode, k, lang))
                    .map(
                        lang ->
                            new LeaseDocument(
                                path(countryCode, k, lang),
                                lang,
                                isAuthoritative(countryCode, lang),
                                nationalLanguages(countryCode).isEmpty())))
        .findFirst();
  }

  /**
   * Whether a document in {@code language} is the authoritative one of the country: the FIRST
   * listed national language only. Documents in the other national languages (BE fr, CH fr/it, FI
   * sv, LU de, CA fr) are translations and carry the courtesy notice; for a country without a
   * national language (CZ) not even the English document is authoritative. Region-dependent
   * authority (Quebec: French by region) needs a pack-level ruling and is not handled here.
   */
  boolean isAuthoritative(String countryCode, String language) {
    return nationalLanguages(countryCode).stream().findFirst().filter(language::equals).isPresent();
  }

  /** The country's national languages in preference order; empty when it has none. */
  List<String> nationalLanguages(String countryCode) {
    return nationalLanguages.getOrDefault(countryCode, List.of());
  }

  /**
   * Languages tried in order: the requested one (when supported), the country's national languages
   * in their listed order, then English; duplicates dropped.
   */
  List<String> languageChain(String countryCode, String requestedLang) {
    Set<String> chain = new LinkedHashSet<>();
    if (requestedLang != null && DocumentLanguages.isSupported(requestedLang)) {
      chain.add(requestedLang);
    }
    chain.addAll(nationalLanguages(countryCode));
    chain.add("en");
    return List.copyOf(chain);
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
