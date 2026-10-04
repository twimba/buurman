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

import com.buurman.service.letters.LeaseDocumentRegistry.ClauseSpec;
import com.buurman.service.letters.LeaseDocumentRegistry.Entry;
import com.buurman.util.DocumentLanguages;

/**
 * Structural consistency of the shipped per-language lease documents, over every entry of {@link
 * LeaseDocumentRegistry} (adding a country is a registry entry, no change here).
 *
 * <p>The database side (every fragment key is an active {@code lease_clause_templates} row for the
 * same country and kind, and vice versa) needs Flyway + Testcontainers: it is {@code
 * LeaseDocumentCatalogDatabaseIntegrationTest}, driven by the same registry. Here the same catalog
 * is checked against the clause table the seed migrations implement (the registry's clause specs
 * mirror them).
 */
@DisplayName("lease document catalog")
class LeaseDocumentCatalogTest {

  private static final String DOCUMENT_ROOT = "templates/documents/lease-agreement/";
  private static final String BUNDLE = "messages/document-lease-agreement";
  private static final Pattern FRAGMENT = Pattern.compile("th:fragment=\"clause-([a-z0-9-]+)\"");
  private static final Pattern CLAUSE_KEY = Pattern.compile("^[a-z0-9-]{1,64}$");
  private static final Pattern FILE =
      Pattern.compile(".*/lease-agreement/([A-Z]{2})/([a-z-]+)/([a-z]{2})\\.html$");

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

  private static Resource document(
      Map<String, Map<String, Resource>> found, Entry entry, String language) {
    return Optional.ofNullable(found.getOrDefault(entry.key(), Map.of()).get(language))
        .orElseThrow(() -> new AssertionError("missing " + entry.key() + "/" + language));
  }

  @Test
  @DisplayName("every catalogued (country, kind) has a document in every enforced language")
  void languageCoverage() throws IOException {
    Map<String, Map<String, Resource>> found = discover();
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      assertThat(found.getOrDefault(entry.key(), Map.of()).keySet())
          .as("%s documents", entry.key())
          .containsAll(entry.enforcedLanguages());
    }
    assertThat(found.keySet())
        .as("every shipped document directory must be catalogued")
        .isSubsetOf(LeaseDocumentRegistry.ENTRIES.stream().map(Entry::key).toList());
  }

  @Test
  @DisplayName(
      "the language files of every catalogued (country, kind) are exactly its enforced languages")
  void languageFilesEqualEnforcedLanguages() throws IOException {
    Map<String, Map<String, Resource>> found = discover();
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      assertThat(found.getOrDefault(entry.key(), Map.of()).keySet())
          .as("%s language files", entry.key())
          .containsExactlyInAnyOrderElementsOf(entry.enforcedLanguages());
    }
  }

  @Test
  @DisplayName("every enforced language declares exactly the catalogued clause fragments")
  void fragmentKeysMatchCatalog() throws IOException {
    Map<String, Map<String, Resource>> found = discover();
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      Set<String> expected = new TreeSet<>(entry.clauseKeys());
      for (String language : entry.enforcedLanguages()) {
        assertThat(new TreeSet<>(fragmentKeys(read(document(found, entry, language)))))
            .as("%s/%s fragments", entry.key(), language)
            .isEqualTo(expected);
      }
    }
  }

  @Test
  @DisplayName("every enforced document starts with the legal header markers")
  void headerMarkers() throws IOException {
    Map<String, Map<String, Resource>> found = discover();
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      for (String language : entry.enforcedLanguages()) {
        String html = read(document(found, entry, language)).stripLeading();
        assertThat(html)
            .as("%s/%s starts with a comment", entry.key(), language)
            .startsWith("<!--");
        String header = html.substring(0, html.indexOf("-->"));
        for (String marker : LeaseDocumentText.HEADER_MARKERS) {
          assertThat(header).as("%s/%s header", entry.key(), language).contains(marker);
        }
      }
    }
  }

  @Test
  @DisplayName(
      "the authoritative document says translation: authoritative, every other one machine-drafted"
          + " with reviewed-by: none")
  void headerTranslationMarkers() throws IOException {
    Map<String, Map<String, Resource>> found = discover();
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      for (String language : entry.enforcedLanguages()) {
        String header = LeaseDocumentText.header(read(document(found, entry, language)));
        assertThat(
                LeaseDocumentText.headerPolicyViolations(
                    header, language.equals(entry.authoritativeLanguage())))
            .as("%s/%s header policy", entry.key(), language)
            .isEmpty();
      }
    }
  }

  @Test
  @DisplayName("catalogued clause keys are safe slugs and pinned clauses sort before the rest")
  void pinnedSortFirst() {
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      List<ClauseSpec> clauses = entry.clauses();
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
      assertThat(maxPinned).as("%s pinned sort orders", entry.key()).isLessThan(minFree);
    }
  }

  @Test
  @DisplayName("every clause has a title and summary in all bundle languages")
  void bundleKeys() throws IOException {
    List<String> missing = new ArrayList<>();
    for (String language : DocumentLanguages.ORDERED) {
      Properties props = bundle(language);
      for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
        for (ClauseSpec clause : entry.clauses()) {
          String prefix = entry.i18nPrefix(clause.key());
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
