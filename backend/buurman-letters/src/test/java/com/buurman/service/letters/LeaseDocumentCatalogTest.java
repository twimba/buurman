package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.buurman.util.DocumentLanguages;

/**
 * Structural consistency of the shipped per-language lease documents.
 *
 * <p>The database side (every fragment key is an active {@code lease_clause_templates} row for the
 * same country and kind, and vice versa) needs Flyway + Testcontainers, which this module does not
 * carry: it lives in {@code buurman-app} as {@code LeaseDocumentCatalogDatabaseIntegrationTest}.
 * Here the same catalog is checked against the clause table the V092 seed implements.
 */
@DisplayName("lease document catalog")
class LeaseDocumentCatalogTest {

  /**
   * Languages whose document must exist for every (country, kind). Task 7 batches add languages
   * here; the final batch sets this to {@link DocumentLanguages#ORDERED}. Until then the locator
   * falls back to the national-language document.
   */
  static final List<String> ENFORCED_LANGUAGES = List.of("nl", "en");

  private static final String DOCUMENT_ROOT = "templates/documents/lease-agreement/";
  private static final String BUNDLE = "messages/document-lease-agreement";
  private static final Pattern FRAGMENT = Pattern.compile("th:fragment=\"clause-([a-z0-9-]+)\"");
  private static final Pattern CLAUSE_KEY = Pattern.compile("^[a-z0-9-]{1,64}$");
  private static final Pattern FILE =
      Pattern.compile(".*/lease-agreement/([A-Z]{2})/([a-z-]+)/([a-z]{2})\\.html$");
  private static final List<String> HEADER_MARKERS =
      List.of("legal-basis:", "reviewed-by:", "translation:");

  private record ClauseSpec(String key, boolean required, boolean pinned, int sortOrder) {}

  /** Mirror of V092__seed_nl_residential_lease_clauses.sql. */
  private static final Map<String, List<ClauseSpec>> CATALOG =
      Map.of(
          "NL/residential",
          List.of(
              new ClauseSpec("parties", true, true, 1),
              new ClauseSpec("premises", true, true, 2),
              new ClauseSpec("term", true, false, 3),
              new ClauseSpec("rent", true, false, 4),
              new ClauseSpec("rent-adjustment", false, false, 5),
              new ClauseSpec("service-costs", false, false, 6),
              new ClauseSpec("deposit", false, false, 7),
              new ClauseSpec("payment", true, false, 8),
              new ClauseSpec("use", false, false, 9),
              new ClauseSpec("subletting", false, false, 10),
              new ClauseSpec("maintenance", false, false, 11),
              new ClauseSpec("energy-label", true, false, 12),
              new ClauseSpec("handover-inspection", false, false, 13),
              new ClauseSpec("termination", true, false, 14),
              new ClauseSpec("data-protection", false, false, 15),
              new ClauseSpec("disputes", false, false, 16)));

  /** "NL/residential" -> language -> document resource; test fixtures are skipped. */
  private static Map<String, Map<String, Resource>> discover() throws IOException {
    Resource[] resources =
        new PathMatchingResourcePatternResolver()
            .getResources("classpath*:" + DOCUMENT_ROOT + "*/*/*.html");
    Map<String, Map<String, Resource>> found = new TreeMap<>();
    for (Resource resource : resources) {
      String url = resource.getURL().toString();
      if (url.contains("/test-classes/")) {
        continue;
      }
      Matcher m = FILE.matcher(url);
      if (!m.matches()) {
        continue;
      }
      found
          .computeIfAbsent(m.group(1) + "/" + m.group(2), k -> new TreeMap<>())
          .put(m.group(3), resource);
    }
    return found;
  }

  private static String read(Resource resource) throws IOException {
    try (InputStream in = resource.getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  static Set<String> fragmentKeys(String html) {
    Set<String> keys = new LinkedHashSet<>();
    Matcher m = FRAGMENT.matcher(html);
    while (m.find()) {
      keys.add(m.group(1));
    }
    return keys;
  }

  private static Properties bundle(String language) throws IOException {
    String name = "en".equals(language) ? BUNDLE : BUNDLE + "_" + language;
    Properties properties = new Properties();
    try (Reader reader =
        new InputStreamReader(
            new ClassPathResource(name + ".properties").getInputStream(), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private static String i18nPrefix(String countryKind, String key) {
    String[] parts = countryKind.split("/");
    return "lease." + parts[0].toLowerCase() + "." + parts[1] + "." + key;
  }

  @Test
  @DisplayName("every catalogued (country, kind) has a document in every enforced language")
  void languageCoverage() throws IOException {
    Map<String, Map<String, Resource>> found = discover();
    for (String countryKind : CATALOG.keySet()) {
      assertThat(found.getOrDefault(countryKind, Map.of()).keySet())
          .as("%s documents", countryKind)
          .containsAll(ENFORCED_LANGUAGES);
    }
    assertThat(found.keySet())
        .as("every shipped document directory must be catalogued")
        .isSubsetOf(CATALOG.keySet());
  }

  @Test
  @DisplayName("every enforced language declares exactly the catalogued clause fragments")
  void fragmentKeysMatchCatalog() throws IOException {
    Map<String, Map<String, Resource>> found = discover();
    for (Map.Entry<String, List<ClauseSpec>> entry : CATALOG.entrySet()) {
      Set<String> expected = new TreeSet<>(entry.getValue().stream().map(ClauseSpec::key).toList());
      for (String language : ENFORCED_LANGUAGES) {
        Resource doc =
            Optional.ofNullable(found.getOrDefault(entry.getKey(), Map.of()).get(language))
                .orElseThrow(
                    () -> new AssertionError("missing " + entry.getKey() + "/" + language));
        assertThat(new TreeSet<>(fragmentKeys(read(doc))))
            .as("%s/%s fragments", entry.getKey(), language)
            .isEqualTo(expected);
      }
    }
  }

  @Test
  @DisplayName("every enforced document starts with the legal header markers")
  void headerMarkers() throws IOException {
    Map<String, Map<String, Resource>> found = discover();
    for (String countryKind : CATALOG.keySet()) {
      for (String language : ENFORCED_LANGUAGES) {
        Resource doc =
            Optional.ofNullable(found.getOrDefault(countryKind, Map.of()).get(language))
                .orElseThrow(() -> new AssertionError("missing " + countryKind + "/" + language));
        String html = read(doc).stripLeading();
        assertThat(html)
            .as("%s/%s starts with a comment", countryKind, language)
            .startsWith("<!--");
        String header = html.substring(0, html.indexOf("-->"));
        for (String marker : HEADER_MARKERS) {
          assertThat(header).as("%s/%s header", countryKind, language).contains(marker);
        }
      }
    }
  }

  @Test
  @DisplayName("catalogued clause keys are safe slugs and pinned clauses sort before the rest")
  void pinnedSortFirst() {
    for (Map.Entry<String, List<ClauseSpec>> entry : CATALOG.entrySet()) {
      List<ClauseSpec> clauses = entry.getValue();
      clauses.forEach(c -> assertThat(c.key()).matches(CLAUSE_KEY));
      int maxPinned =
          clauses.stream()
              .filter(ClauseSpec::pinned)
              .mapToInt(ClauseSpec::sortOrder)
              .max()
              .orElse(0);
      int minFree =
          clauses.stream()
              .filter(c -> !c.pinned())
              .mapToInt(ClauseSpec::sortOrder)
              .min()
              .orElse(Integer.MAX_VALUE);
      assertThat(maxPinned).as("%s pinned sort orders", entry.getKey()).isLessThan(minFree);
    }
  }

  @Test
  @DisplayName("every clause has a title and summary in all bundle languages")
  void bundleKeys() throws IOException {
    List<String> missing = new ArrayList<>();
    for (String language : DocumentLanguages.ORDERED) {
      Properties props = bundle(language);
      for (Map.Entry<String, List<ClauseSpec>> entry : CATALOG.entrySet()) {
        for (ClauseSpec clause : entry.getValue()) {
          String prefix = i18nPrefix(entry.getKey(), clause.key());
          for (String key : List.of(prefix + ".title", prefix + ".summary")) {
            String value = props.getProperty(key);
            if (value == null || value.isBlank()) {
              missing.add(language + ":" + key);
            }
          }
        }
      }
    }
    assertThat(missing).as("missing bundle keys").isEmpty();
  }
}
