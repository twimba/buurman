package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Mechanical fidelity gate: a translated lease document may change prose only. Against the
 * authoritative {@code nl.html} it must keep the same fragments, the same Thymeleaf expressions and
 * structural attributes in the same order, the same digits and statute citations per fragment, and
 * the legal header markers.
 */
@DisplayName("lease document fidelity")
class LeaseDocumentFidelityTest {

  private static final String DOCUMENT_ROOT = "templates/documents/lease-agreement/";
  private static final List<String> COUNTRY_KINDS = List.of("NL/residential");

  private static final Pattern FRAGMENT_START = Pattern.compile("th:fragment=\"([^\"]+)\"");
  private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
  private static final Pattern STRUCTURE =
      Pattern.compile("</?th:[\\w-]+|(?:th:[\\w-]+|data-[\\w-]+)=\"[^\"]*\"");
  private static final Pattern TAG = Pattern.compile("<[^>]*>");
  private static final Pattern ENTITY = Pattern.compile("&[#a-zA-Z0-9]+;");
  private static final Pattern NUMBER = Pattern.compile("\\d+(?::\\d+[a-z]?)?");
  private static final Pattern HEADER_ANCHOR =
      Pattern.compile(
          "\\d{4}-\\d{2}-\\d{2}|[a-z][a-z0-9-]*(?:\\.[a-z0-9-]+)*\\.(?:nl|eu)\\b|BWBR\\d+|CELEX\\s+\\w+");
  private static final List<String> REQUIRED_HEADER =
      List.of("legal-basis:", "reviewed-by: none", "translation: machine-drafted");

  /** All violations of {@code other} against the authoritative {@code nl} document. */
  static List<String> violations(String nl, String other) {
    List<String> problems = new ArrayList<>();
    problems.addAll(headerViolations(nl, other));

    Map<String, String> nlFragments = fragments(nl);
    Map<String, String> otherFragments = fragments(other);
    if (!nlFragments.keySet().equals(otherFragments.keySet())) {
      problems.add(
          "fragments differ: expected "
              + nlFragments.keySet()
              + " but was "
              + otherFragments.keySet());
      return problems;
    }
    nlFragments.forEach(
        (name, nlBody) -> {
          String otherBody = otherFragments.get(name);
          List<String> nlStructure = structure(nlBody);
          List<String> otherStructure = structure(otherBody);
          if (!nlStructure.equals(otherStructure)) {
            problems.add(
                "expressions differ in "
                    + name
                    + ": expected "
                    + nlStructure
                    + " but was "
                    + otherStructure);
          }
          List<String> nlNumbers = numbers(nlBody);
          List<String> otherNumbers = numbers(otherBody);
          if (!nlNumbers.equals(otherNumbers)) {
            problems.add(
                "numbers differ in "
                    + name
                    + ": expected "
                    + nlNumbers
                    + " but was "
                    + otherNumbers);
          }
        });
    return problems;
  }

  private static List<String> headerViolations(String nl, String other) {
    List<String> problems = new ArrayList<>();
    String header = header(other);
    for (String marker : REQUIRED_HEADER) {
      if (!header.contains(marker)) {
        problems.add("header marker missing: " + marker);
      }
    }
    Matcher m = HEADER_ANCHOR.matcher(legalBasis(header(nl)));
    while (m.find()) {
      if (!header.contains(m.group())) {
        problems.add("header anchor missing: " + m.group());
      }
    }
    return problems;
  }

  private static String header(String html) {
    String trimmed = html.stripLeading();
    if (!trimmed.startsWith("<!--")) {
      return "";
    }
    return trimmed.substring(0, Math.max(trimmed.indexOf("-->"), 0));
  }

  /** The legal-basis block: from its marker up to the reviewed-by marker. */
  private static String legalBasis(String header) {
    int start = header.indexOf("legal-basis:");
    int end = header.indexOf("reviewed-by:");
    return start < 0 ? "" : header.substring(start, end < start ? header.length() : end);
  }

  /** Fragment name to the markup between its declaration and the next one (or end of file). */
  static Map<String, String> fragments(String html) {
    String body = COMMENT.matcher(html).replaceAll("");
    Map<String, String> result = new LinkedHashMap<>();
    Matcher m = FRAGMENT_START.matcher(body);
    List<int[]> bounds = new ArrayList<>();
    List<String> names = new ArrayList<>();
    while (m.find()) {
      names.add(m.group(1));
      bounds.add(new int[] {m.start(), 0});
    }
    for (int i = 0; i < names.size(); i++) {
      int end = i + 1 < names.size() ? bounds.get(i + 1)[0] : body.length();
      result.put(names.get(i), body.substring(bounds.get(i)[0], end));
    }
    return result;
  }

  /** Ordered Thymeleaf tags and attributes, plus data-* markers, whitespace normalized. */
  static List<String> structure(String fragment) {
    List<String> result = new ArrayList<>();
    Matcher m = STRUCTURE.matcher(fragment);
    while (m.find()) {
      result.add(m.group().replaceAll("\\s+", " "));
    }
    return result;
  }

  /** Sorted multiset of digit sequences and statute citations in the prose. */
  static List<String> numbers(String fragment) {
    String prose = ENTITY.matcher(TAG.matcher(fragment).replaceAll(" ")).replaceAll(" ");
    List<String> result = new ArrayList<>();
    Matcher m = NUMBER.matcher(prose);
    while (m.find()) {
      result.add(m.group());
    }
    Collections.sort(result);
    return result;
  }

  private static String read(String country, String kind, String language) throws IOException {
    try (var in =
        new ClassPathResource(DOCUMENT_ROOT + country + "/" + kind + "/" + language + ".html")
            .getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  @DisplayName("every enforced translation is faithful to the authoritative NL document")
  void translationsAreFaithful() throws IOException {
    for (String countryKind : COUNTRY_KINDS) {
      String[] parts = countryKind.split("/");
      String nl = read(parts[0], parts[1], "nl");
      for (String language : LeaseDocumentCatalogTest.ENFORCED_LANGUAGES) {
        if ("nl".equals(language)) {
          continue;
        }
        assertThat(violations(nl, read(parts[0], parts[1], language)))
            .as("%s/%s fidelity", countryKind, language)
            .isEmpty();
      }
    }
  }

  // ---- the checker itself: each check must bite on synthetic input ----

  private static final String NL =
      """
      <!--
        legal-basis: BW 7:271 wetten.overheid.nl Verified 2026-10-03.
        reviewed-by: none
        translation: authoritative
      -->
      <div th:fragment="clause-a" data-clause="a">
        <p th:if="${x != null}">Zie artikel 7:271 lid 2 BW, <strong th:text="${y}">1</strong> maand.</p>
      </div>
      <div th:fragment="clause-b" data-clause="b">
        <p th:unless="${z}">Bedrag 250 euro.</p>
      </div>
      """;

  private static final String GOOD =
      """
      <!--
        legal-basis: BW 7:271 wetten.overheid.nl Verified 2026-10-03.
        reviewed-by: none
        translation: machine-drafted
      -->
      <div th:fragment="clause-a" data-clause="a">
        <p
          th:if="${x != null}">See article 7:271 paragraph 2, <strong th:text="${y}">1</strong> month.</p>
      </div>
      <div th:fragment="clause-b" data-clause="b">
        <p th:unless="${z}">Amount of 250 euro.</p>
      </div>
      """;

  @Test
  @DisplayName("checker accepts a faithful translation, including reflowed whitespace")
  void checkerAcceptsFaithful() {
    assertThat(violations(NL, GOOD)).isEmpty();
  }

  @Test
  @DisplayName("checker fails when a fragment is dropped or invented")
  void checkerFlagsFragmentSet() {
    String dropped = GOOD.substring(0, GOOD.indexOf("<div th:fragment=\"clause-b\""));
    assertThat(violations(NL, dropped)).anyMatch(v -> v.startsWith("fragments differ"));
    String invented = GOOD + "<div th:fragment=\"clause-c\"><p>x</p></div>";
    assertThat(violations(NL, invented)).anyMatch(v -> v.startsWith("fragments differ"));
  }

  @Test
  @DisplayName("checker fails when an expression changes, is dropped, or is reordered")
  void checkerFlagsExpressions() {
    assertThat(violations(NL, GOOD.replace("${x != null}", "${x == null}")))
        .anyMatch(v -> v.startsWith("expressions differ in clause-a"));
    assertThat(violations(NL, GOOD.replace(" th:unless=\"${z}\"", "")))
        .anyMatch(v -> v.startsWith("expressions differ in clause-b"));
    assertThat(violations(NL, GOOD.replace("th:text=\"${y}\"", "th:text=\"${y}\" th:if=\"${q}\"")))
        .anyMatch(v -> v.startsWith("expressions differ in clause-a"));
    assertThat(violations(NL, GOOD.replace("data-clause=\"b\"", "data-clause=\"c\"")))
        .anyMatch(v -> v.startsWith("expressions differ in clause-b"));
  }

  @Test
  @DisplayName("checker fails when a number or citation is dropped, invented or altered")
  void checkerFlagsNumbers() {
    assertThat(violations(NL, GOOD.replace("250", "205")))
        .anyMatch(v -> v.startsWith("numbers differ in clause-b"));
    assertThat(violations(NL, GOOD.replace("7:271 paragraph 2", "7:271")))
        .anyMatch(v -> v.startsWith("numbers differ in clause-a"));
    assertThat(violations(NL, GOOD.replace("Amount of 250 euro.", "Amount of 250 euro in 2026.")))
        .anyMatch(v -> v.startsWith("numbers differ in clause-b"));
    assertThat(violations(NL, GOOD.replace("7:271 paragraph", "7:272 paragraph")))
        .anyMatch(v -> v.startsWith("numbers differ in clause-a"));
  }

  @Test
  @DisplayName("checker fails when a header marker or the verification anchors are missing")
  void checkerFlagsHeader() {
    assertThat(
            violations(
                NL, GOOD.replace("translation: machine-drafted", "translation: authoritative")))
        .contains("header marker missing: translation: machine-drafted");
    assertThat(violations(NL, GOOD.replace("reviewed-by: none", "reviewed-by: counsel")))
        .contains("header marker missing: reviewed-by: none");
    assertThat(violations(NL, GOOD.replace("legal-basis:", "basis:")))
        .contains("header marker missing: legal-basis:");
    assertThat(violations(NL, GOOD.replace("2026-10-03", "2026-10-04")))
        .contains("header anchor missing: 2026-10-03");
    assertThat(violations(NL, GOOD.replace("wetten.overheid.nl", "example")))
        .contains("header anchor missing: wetten.overheid.nl");
  }
}
