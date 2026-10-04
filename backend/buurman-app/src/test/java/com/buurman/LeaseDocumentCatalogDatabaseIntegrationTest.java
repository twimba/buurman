package com.buurman;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.testcontainers.containers.PostgreSQLContainer;

import com.buurman.domain.LeaseKind;
import com.buurman.service.letters.LeaseDocumentRegistry;
import com.buurman.service.letters.LeaseDocumentRegistry.ClauseSpec;
import com.buurman.service.letters.LeaseDocumentRegistry.Entry;
import com.buurman.util.DocumentLanguages;

/**
 * The shipped lease documents and the Flyway-seeded {@code lease_clause_templates} rows must agree,
 * for every entry of {@link LeaseDocumentRegistry} (the registry is shared with the letters module
 * through its test-jar; adding a country is a registry entry, no change here): every {@code
 * clause-<key>} fragment of every enforced language is an active row for the same country and kind
 * and vice versa, the rows match the registry's clause specs (required, pinned, order), pinned
 * clauses sort first, and every row's title/summary key exists in all 13 bundles.
 *
 * <p>Lives here rather than in {@code buurman-letters} because only this module has both the letter
 * templates and Testcontainers on its test classpath. Mirrors the container setup of {@code
 * AbstractRepositoryIntegrationTest} but does not truncate, since it asserts on the seed. Docker
 * must be running.
 */
@DisplayName("lease documents agree with the seeded clause templates")
class LeaseDocumentCatalogDatabaseIntegrationTest {

  @SuppressWarnings("resource")
  private static final PostgreSQLContainer<?> PG =
      new PostgreSQLContainer<>("postgres:18-alpine")
          .withDatabaseName("buurman_catalog_test")
          .withUsername("buurman")
          .withPassword("buurman");

  private static final DSLContext DSL_CONTEXT;

  private static final Pattern FRAGMENT = Pattern.compile("th:fragment=\"clause-([a-z0-9-]+)\"");
  private static final Pattern FILE =
      Pattern.compile(
          ".*/templates/documents/lease-agreement/([A-Z]{2})/([a-z-]+)/([a-z]{2})\\.html$");

  static {
    PG.start();
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(PG.getJdbcUrl());
    ds.setUser(PG.getUsername());
    ds.setPassword(PG.getPassword());
    Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
    DSL_CONTEXT = DSL.using((DataSource) ds, SQLDialect.POSTGRES);
  }

  private record Row(
      String clauseKey,
      String titleKey,
      String bodyKey,
      boolean optional,
      boolean defaultIncluded,
      boolean pinned,
      int sortOrder) {}

  /** "NL/RESIDENTIAL" -> fragment keys of each language document found. */
  private static Map<String, Map<String, Set<String>>> documents() throws IOException {
    Resource[] resources =
        new PathMatchingResourcePatternResolver()
            .getResources("classpath*:templates/documents/lease-agreement/*/*/*.html");
    Map<String, Map<String, Set<String>>> found = new TreeMap<>();
    for (Resource resource : resources) {
      if (resource.getURL().toString().contains("/test-classes/")) {
        continue;
      }
      Matcher file = FILE.matcher(resource.getURL().toString());
      if (!file.matches()) {
        continue;
      }
      String kind = file.group(2).toUpperCase(Locale.ROOT).replace('-', '_');
      String html;
      try (InputStream in = resource.getInputStream()) {
        html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
      Set<String> keys = new TreeSet<>();
      Matcher m = FRAGMENT.matcher(html);
      while (m.find()) {
        keys.add(m.group(1));
      }
      found
          .computeIfAbsent(
              file.group(1) + "/" + LeaseKind.valueOf(kind).name(), k -> new TreeMap<>())
          .put(file.group(3), keys);
    }
    return found;
  }

  private static List<Row> rows(String country, String kind) {
    return DSL_CONTEXT
        .select()
        .from(DSL.table("lease_clause_templates"))
        .where(DSL.field("country_code").eq(country))
        .and(DSL.field("lease_kind").eq(kind))
        .and(DSL.field("deleted_at").isNull())
        .fetch()
        .map(LeaseDocumentCatalogDatabaseIntegrationTest::toRow);
  }

  private static Row toRow(Record r) {
    return new Row(
        r.get("clause_key", String.class),
        r.get("title_i18n_key", String.class),
        r.get("body_i18n_key", String.class),
        r.get("optional", Boolean.class),
        r.get("default_included", Boolean.class),
        r.get("pinned", Boolean.class),
        r.get("sort_order", Integer.class));
  }

  private static Properties bundle(String language) throws IOException {
    String name =
        "messages/document-lease-agreement" + ("en".equals(language) ? "" : "_" + language);
    Properties properties = new Properties();
    try (Reader reader =
        new InputStreamReader(
            new ClassPathResource(name + ".properties").getInputStream(), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  @Test
  @DisplayName(
      "every registered (country, kind) is documented in its enforced languages and seeded")
  void registeredEntriesPresent() throws IOException {
    Map<String, Map<String, Set<String>>> documents = documents();
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      assertThat(documents).as("%s documented", entry.dbKey()).containsKey(entry.dbKey());
      assertThat(documents.getOrDefault(entry.dbKey(), Map.of()).keySet())
          .as("%s language files", entry.dbKey())
          .containsExactlyInAnyOrderElementsOf(entry.enforcedLanguages());
      assertThat(rows(entry.countryCode(), entry.kind().name()))
          .as("%s seeded rows", entry.dbKey())
          .hasSize(entry.clauses().size());
    }
  }

  @Test
  @DisplayName("every shipped document directory is registered")
  void everyDocumentIsRegistered() throws IOException {
    assertThat(documents().keySet())
        .isSubsetOf(LeaseDocumentRegistry.ENTRIES.stream().map(Entry::dbKey).toList());
  }

  @Test
  @DisplayName("every document's fragments equal the active rows of its country and kind")
  void fragmentsEqualRows() throws IOException {
    Map<String, Map<String, Set<String>>> documents = documents();
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      Set<String> rowKeys =
          new TreeSet<>(
              rows(entry.countryCode(), entry.kind().name()).stream().map(Row::clauseKey).toList());
      Set<String> specKeys = new TreeSet<>(entry.clauseKeys());
      assertThat(rowKeys).as("%s rows vs registry clause keys", entry.dbKey()).isEqualTo(specKeys);
      for (String language : entry.enforcedLanguages()) {
        assertThat(documents.getOrDefault(entry.dbKey(), Map.of()).get(language))
            .as("%s/%s fragments vs rows", entry.dbKey(), language)
            .isEqualTo(rowKeys);
      }
    }
    for (Map.Entry<String, Map<String, Set<String>>> doc : documents.entrySet()) {
      String[] ck = doc.getKey().split("/");
      Set<String> rowKeys = new TreeSet<>(rows(ck[0], ck[1]).stream().map(Row::clauseKey).toList());
      doc.getValue()
          .forEach(
              (language, keys) ->
                  assertThat(keys)
                      .as("%s/%s fragments vs rows", doc.getKey(), language)
                      .isEqualTo(rowKeys));
    }
  }

  @Test
  @DisplayName("every non-legacy seeded (country, kind) has a document and a registry entry")
  void everySeededKindHasDocument() throws IOException {
    Set<String> documented = documents().keySet();
    List<String> seeded =
        DSL_CONTEXT
            .selectDistinct(DSL.field("country_code"), DSL.field("lease_kind"))
            .from(DSL.table("lease_clause_templates"))
            .where(DSL.field("deleted_at").isNull())
            .and(DSL.field("lease_kind").ne(LeaseKind.LEGACY.name()))
            .fetch(r -> r.get(0, String.class) + "/" + r.get(1, String.class));
    assertThat(documented).containsAll(seeded);
    assertThat(LeaseDocumentRegistry.ENTRIES.stream().map(Entry::dbKey).toList())
        .as("seeded kinds without a registry entry")
        .containsAll(seeded);
  }

  @Test
  @DisplayName("rows match the registry clause specs; pinned rows sort first; required default in")
  void pinnedAndRequired() {
    for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
      List<Row> rows = rows(entry.countryCode(), entry.kind().name());
      for (ClauseSpec spec : entry.clauses()) {
        Row row =
            rows.stream()
                .filter(r -> r.clauseKey().equals(spec.key()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(entry.dbKey() + " has no row " + spec.key()));
        assertThat(row.optional())
            .as("%s/%s optional", entry.dbKey(), spec.key())
            .isEqualTo(!spec.required());
        assertThat(row.pinned())
            .as("%s/%s pinned", entry.dbKey(), spec.key())
            .isEqualTo(spec.pinned());
        assertThat(row.sortOrder())
            .as("%s/%s sort order", entry.dbKey(), spec.key())
            .isEqualTo(spec.sortOrder());
      }
      int maxPinned = rows.stream().filter(Row::pinned).mapToInt(Row::sortOrder).max().orElse(0);
      int minFree =
          rows.stream()
              .filter(r -> !r.pinned())
              .mapToInt(Row::sortOrder)
              .min()
              .orElse(Integer.MAX_VALUE);
      assertThat(maxPinned).as("%s pinned sort orders", entry.dbKey()).isLessThan(minFree);
      rows.stream()
          .filter(r -> !r.optional())
          .forEach(r -> assertThat(r.defaultIncluded()).as(r.clauseKey()).isTrue());
    }
  }

  @Test
  @DisplayName(
      "every row's title and summary key follows the registry prefix and exists in all 13 bundles")
  void rowBundleKeys() throws IOException {
    List<String> missing = new ArrayList<>();
    for (String language : DocumentLanguages.ORDERED) {
      Properties props = bundle(language);
      for (Entry entry : LeaseDocumentRegistry.ENTRIES) {
        for (Row row : rows(entry.countryCode(), entry.kind().name())) {
          String prefix = entry.i18nPrefix(row.clauseKey());
          assertThat(row.titleKey())
              .as("%s title key", row.clauseKey())
              .isEqualTo(prefix + ".title");
          assertThat(row.bodyKey())
              .as("%s summary key", row.clauseKey())
              .isEqualTo(prefix + ".summary");
          for (String key : List.of(row.titleKey(), row.bodyKey())) {
            String value = props.getProperty(key);
            if (value == null || value.isBlank()) {
              missing.add(language + ":" + key);
            }
          }
        }
      }
    }
    assertThat(missing).isEmpty();
  }
}
