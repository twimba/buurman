package com.buurman;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.buurman.util.DocumentLanguages;

/**
 * Every message bundle must carry every key in every language, with the same placeholders.
 *
 * <p>Families are discovered from the classpath rather than listed, so a new bundle is covered the
 * day it is added. The notification MessageSource sets useCodeAsDefaultMessage(true), which means a
 * missing key is not a blank — it renders the literal key into a delivered email.
 */
@DisplayName("every message bundle is complete in every language")
class I18nBundleParityTest {

  /** {0}-style MessageFormat arguments and {named} SMS tokens alike. */
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

  private static final String BASE_LANGUAGE = "en";

  /**
   * The {@code family:language} pairs allowed to drop a token, because their copy is under a hard
   * length budget. Only Greek SMS qualifies: its script cannot fold to GSM-7, so it gets 70 UTF-16
   * units, and the worst-case property name and contact name alone spend 50 of them — carrying
   * every token is not physically possible. The design records this as the deliberate tradeoff
   * ("Greek copy is authored terse against that budget, dropping the property name where it does
   * not fit").
   *
   * <p>Scoped per language, not per family: a Dutch retranslation that dropped {@code {amount}}
   * from a payment SMS would ship a reminder with no amount in it, and must still fail.
   */
  private static final Set<String> BUDGETED = Set.of("sms-bodies:el");

  private record Family(String name, Map<String, Resource> byLanguage) {}

  private static List<Family> discover() throws IOException {
    Resource[] resources =
        new PathMatchingResourcePatternResolver().getResources("classpath*:messages/*.properties");

    Map<String, Map<String, Resource>> families = new LinkedHashMap<>();
    for (Resource resource : resources) {
      String fileName = resource.getFilename();
      if (fileName == null) {
        continue;
      }
      String base = fileName.substring(0, fileName.length() - ".properties".length());
      // Fixtures used by other tests, not shipped copy.
      if (base.startsWith("test-")) {
        continue;
      }
      String family = base;
      String language = BASE_LANGUAGE;
      int underscore = base.lastIndexOf('_');
      if (underscore > 0 && base.length() - underscore == 3) {
        family = base.substring(0, underscore);
        language = base.substring(underscore + 1);
      }
      families.computeIfAbsent(family, key -> new LinkedHashMap<>()).put(language, resource);
    }

    List<Family> discovered = new ArrayList<>();
    families.forEach((name, byLanguage) -> discovered.add(new Family(name, byLanguage)));
    return discovered;
  }

  private static Resource bundleFor(Family family, String language) {
    return Optional.ofNullable(family.byLanguage().get(language))
        .orElseThrow(
            () -> new AssertionError("%s has no %s bundle".formatted(family.name(), language)));
  }

  private static Properties load(Resource resource) throws IOException {
    Properties properties = new Properties();
    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private static Set<String> placeholders(String value) {
    Set<String> found = new TreeSet<>();
    Matcher matcher = PLACEHOLDER.matcher(value);
    while (matcher.find()) {
      found.add(matcher.group(1));
    }
    return found;
  }

  static Stream<Arguments> families() throws IOException {
    List<Family> discovered = discover();
    // Pinned exactly: a floor lets a classpath-scanning regression lose a whole family in
    // silence, which is the one failure that would make every assertion below vacuous.
    assertThat(discovered)
        .as("expected every message bundle family to be discovered")
        .extracting(Family::name)
        .containsExactlyInAnyOrder(
            "api-errors",
            "document-contact-booklet",
            "document-contract-booklet",
            "document-deposit-statement",
            "document-enum-labels",
            "document-extension",
            "document-letter-chrome",
            "document-payment-notice",
            "document-property-booklet",
            "document-rent-change",
            "document-summary-card",
            "email-bodies",
            "email-subjects",
            "sms-bodies");
    return discovered.stream().map(Arguments::of);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  @DisplayName("carries every language")
  void carriesEveryLanguage(Family family) {
    assertThat(family.byLanguage().keySet())
        .as("%s is missing language files", family.name())
        .containsAll(DocumentLanguages.ORDERED);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("families")
  @DisplayName("every language carries every key, with the same placeholders")
  void everyLanguageCarriesEveryKey(Family family) throws IOException {
    Properties base = load(bundleFor(family, BASE_LANGUAGE));
    Set<String> baseKeys = new TreeSet<>(base.stringPropertyNames());

    for (String language : DocumentLanguages.ORDERED) {
      if (BASE_LANGUAGE.equals(language)) {
        continue;
      }
      Properties translated = load(bundleFor(family, language));
      Set<String> translatedKeys = new TreeSet<>(translated.stringPropertyNames());

      assertThat(translatedKeys)
          .as("%s [%s] is missing keys", family.name(), language)
          .containsAll(baseKeys);
      assertThat(baseKeys)
          .as("%s [%s] has keys the base bundle does not", family.name(), language)
          .containsAll(translatedKeys);

      for (String key : baseKeys) {
        Set<String> expected = placeholders(base.getProperty(key));
        Set<String> actual = placeholders(translated.getProperty(key));

        // Introducing a token is always a bug: nothing supplies it, so it is delivered raw.
        assertThat(actual)
            .as(
                "%s [%s] %s introduced a placeholder the English copy does not have — nothing"
                    + " supplies it, so it would be delivered raw",
                family.name(), language, key)
            .isSubsetOf(expected);

        if (!BUDGETED.contains(family.name() + ":" + language)) {
          assertThat(actual)
              .as("%s [%s] %s dropped a placeholder", family.name(), language, key)
              .isEqualTo(expected);
        }
      }
    }
  }
}
