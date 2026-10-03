package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Mechanical fidelity gate: a translated lease document may change prose only. Against the
 * authoritative {@code nl.html} it must keep the same fragments, the same Thymeleaf expressions and
 * structural attributes in the same order, the same element/text skeleton, the same statute
 * citations (ordered) and bare numbers (multiset) per fragment, and the legal header.
 *
 * <p>Convention that makes the number check meaningful: every numeric quantity in the prose
 * (deadlines, periods, multiples, amounts, counts) is written as digits in every document. {@code
 * nl.html} is itself checked for Dutch number words; "een" is allowed because it is the indefinite
 * article, and "in acht nemen" because "acht" there means "heed", not eight.
 */
@DisplayName("lease document fidelity")
class LeaseDocumentFidelityTest {

  private static final String DOCUMENT_ROOT = "templates/documents/lease-agreement/";
  private static final List<String> COUNTRY_KINDS = List.of("NL/residential");

  private static final Pattern FRAGMENT_START = Pattern.compile("th:fragment=\"([^\"]+)\"");
  private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
  private static final Pattern STRUCTURE =
      Pattern.compile("</?th:[\\w-]+|(?:th:[\\w-]+|data-[\\w-]+)=\"[^\"]*\"");
  private static final Pattern TAG =
      Pattern.compile("<(/?)([a-zA-Z][\\w:-]*)(?:\"[^\"]*\"|'[^']*'|[^>\"'])*>");
  private static final Pattern ENTITY = Pattern.compile("&[#a-zA-Z0-9]+;");
  private static final Pattern CITATION = Pattern.compile("\\d+:\\d+[a-z]?");
  private static final Pattern NUMBER = Pattern.compile("\\d+");
  private static final Pattern HEADER_ANCHOR =
      Pattern.compile(
          "\\d{4}-\\d{2}-\\d{2}|\\d{2}-\\d{2}-\\d{4}|7:\\d+[a-z]?"
              + "|[a-z][a-z0-9-]*(?:\\.[a-z0-9-]+)*\\.(?:nl|eu)\\b|BWBR\\d+|CELEX\\s+\\w+");
  private static final Pattern TRANSLATION = Pattern.compile("translation:\\s*(\\S+)");
  private static final Pattern REVIEWED_BY = Pattern.compile("reviewed-by:\\s*(\\S+)");
  private static final Pattern IN_ACHT = Pattern.compile("\\bin acht\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern DUTCH_NUMBER_WORD =
      Pattern.compile(
          "\\b(twee|drie|vier|vijf|zes|zeven|acht|negen|tien|elf|twaalf|dertien|veertien|vijftien"
              + "|twintig|dertig|veertig|vijftig|zestig|negentig|honderd|duizend|eenmaal|tweemaal"
              + "|driemaal|half|halve"
              + "|een (?:maand|maanden|week|weken|jaar|jaren))\\b",
          Pattern.CASE_INSENSITIVE);

  /** All violations of {@code other} against the authoritative {@code nl} document. */
  static List<String> violations(String nl, String other) {
    List<String> problems = new ArrayList<>(headerViolations(nl, other));

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
          compare(problems, "expressions", name, structure(nlBody), structure(otherBody));
          compare(problems, "skeleton", name, skeleton(nlBody), skeleton(otherBody));
          compare(problems, "citations", name, citations(nlBody), citations(otherBody));
          compare(problems, "numbers", name, numbers(nlBody), numbers(otherBody));
        });
    return problems;
  }

  private static void compare(
      List<String> problems, String what, String fragment, List<String> nl, List<String> other) {
    if (!nl.equals(other)) {
      problems.add(what + " differ in " + fragment + ": expected " + nl + " but was " + other);
    }
  }

  /** Dutch number words left in the prose of the authoritative document. */
  static List<String> numberWordViolations(String nl) {
    List<String> problems = new ArrayList<>();
    fragments(nl)
        .forEach(
            (name, body) -> {
              String prose = IN_ACHT.matcher(prose(body)).replaceAll(" ");
              Matcher m = DUTCH_NUMBER_WORD.matcher(prose);
              while (m.find()) {
                problems.add("number word in " + name + ": " + m.group());
              }
            });
    return problems;
  }

  private static List<String> headerViolations(String nl, String other) {
    List<String> problems = new ArrayList<>();
    String header = header(other);
    for (String marker : List.of("legal-basis:", "reviewed-by:", "translation:")) {
      if (!header.contains(marker)) {
        problems.add("header marker missing: " + marker);
      }
    }
    Optional<String> translation = value(TRANSLATION, header);
    Optional<String> reviewedBy = value(REVIEWED_BY, header);
    if (translation.isPresent() && reviewedBy.isPresent()) {
      boolean machine = translation.get().equals("machine-drafted");
      if (machine != reviewedBy.get().equals("none")) {
        problems.add("header rule: translation machine-drafted <=> reviewed-by none");
      }
      if (translation.get().equals("authoritative")) {
        problems.add("header rule: a translation must not claim translation: authoritative");
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

  private static Optional<String> value(Pattern pattern, String header) {
    Matcher m = pattern.matcher(header);
    return m.find() ? Optional.of(m.group(1)) : Optional.empty();
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

  /** Fragment name to the markup from its opening tag up to the next fragment's opening tag. */
  static Map<String, String> fragments(String html) {
    String body = COMMENT.matcher(html).replaceAll("");
    Map<String, String> result = new LinkedHashMap<>();
    Matcher m = FRAGMENT_START.matcher(body);
    List<Integer> starts = new ArrayList<>();
    List<String> names = new ArrayList<>();
    while (m.find()) {
      names.add(m.group(1));
      starts.add(body.lastIndexOf('<', m.start()));
    }
    for (int i = 0; i < names.size(); i++) {
      int end = i + 1 < names.size() ? starts.get(i + 1) : body.length();
      result.put(names.get(i), body.substring(starts.get(i), end));
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

  /**
   * Language-neutral skeleton: ordered opening/closing tag names, with a text marker wherever
   * non-blank text sits between tags. Prose cannot move in or out of a conditional element.
   */
  static List<String> skeleton(String fragment) {
    List<String> result = new ArrayList<>();
    Matcher m = TAG.matcher(fragment);
    int last = 0;
    while (m.find()) {
      if (!fragment.substring(last, m.start()).isBlank()) {
        result.add("#text");
      }
      result.add(m.group(1) + m.group(2).toLowerCase());
      last = m.end();
    }
    if (!fragment.substring(last).isBlank()) {
      result.add("#text");
    }
    return result;
  }

  private static String prose(String fragment) {
    return ENTITY.matcher(TAG.matcher(fragment).replaceAll(" ")).replaceAll(" ");
  }

  /** Statute citations such as {@code 7:261b}, in document order. */
  static List<String> citations(String fragment) {
    List<String> result = new ArrayList<>();
    Matcher m = CITATION.matcher(prose(fragment));
    while (m.find()) {
      result.add(m.group());
    }
    return result;
  }

  /** Sorted multiset of the remaining digit sequences in the prose. */
  static List<String> numbers(String fragment) {
    List<String> result = new ArrayList<>();
    Matcher m = NUMBER.matcher(CITATION.matcher(prose(fragment)).replaceAll(" "));
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

  @Test
  @DisplayName("nl.html is authoritative and writes every quantity as digits")
  void nlIsAuthoritativeAndDigitOnly() throws IOException {
    for (String countryKind : COUNTRY_KINDS) {
      String[] parts = countryKind.split("/");
      String nl = read(parts[0], parts[1], "nl");
      assertThat(header(nl)).containsPattern("translation:\\s*authoritative");
      assertThat(numberWordViolations(nl)).as("%s nl number words", countryKind).isEmpty();
    }
  }

  // ---- the checker itself: each check must bite on synthetic input ----

  private static final String NL =
      """
      <!--
        legal-basis: BW 7:271 wetten.overheid.nl Verified 2026-10-03, in force 01-07-2024, art. 7:249.
        reviewed-by: none
        translation: authoritative
      -->
      <div th:fragment="clause-a" data-clause="a">
        <p th:if="${x != null}">Zie artikel 7:271 lid 2 BW en 7:249, <strong th:text="${y}">1</strong> maand.</p>
        <p>Basis. <span th:if="${f}">Extra zin.</span></p>
      </div>
      <div th:fragment="clause-b" data-clause="b">
        <p th:unless="${z}" th:text="${w}">Bedrag 250 euro.</p>
      </div>
      """;

  private static final String GOOD =
      """
      <!--
        legal-basis: BW 7:271 wetten.overheid.nl Verified 2026-10-03, in force 01-07-2024, art. 7:249.
        reviewed-by: none
        translation: machine-drafted
      -->
      <div th:fragment="clause-a" data-clause="a">
        <p
          th:if="${x != null}">See article 7:271 paragraph 2 and 7:249, <strong th:text="${y}">1</strong> month.</p>
        <p>Basis. <span th:if="${f}">Extra sentence.</span></p>
      </div>
      <div th:fragment="clause-b" data-clause="b">
        <p th:unless="${z}" th:text="${w}">Amount of 250 euro.</p>
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
  @DisplayName("checker fails when an expression changes, is dropped, swapped or reordered")
  void checkerFlagsExpressions() {
    assertThat(violations(NL, GOOD.replace("${x != null}", "${x == null}")))
        .anyMatch(v -> v.startsWith("expressions differ in clause-a"));
    assertThat(violations(NL, GOOD.replace(" th:unless=\"${z}\"", "")))
        .anyMatch(v -> v.startsWith("expressions differ in clause-b"));
    assertThat(violations(NL, GOOD.replace("th:text=\"${y}\"", "th:text=\"${y}\" th:if=\"${q}\"")))
        .anyMatch(v -> v.startsWith("expressions differ in clause-a"));
    assertThat(violations(NL, GOOD.replace("data-clause=\"b\"", "data-clause=\"c\"")))
        .anyMatch(v -> v.startsWith("expressions differ in clause-b"));
    // two attributes of one element swapped
    assertThat(
            violations(
                NL,
                GOOD.replace(
                    "th:unless=\"${z}\" th:text=\"${w}\"", "th:text=\"${w}\" th:unless=\"${z}\"")))
        .anyMatch(v -> v.startsWith("expressions differ in clause-b"));
  }

  @Test
  @DisplayName("checker fails when prose moves in or out of a conditional element or tags change")
  void checkerFlagsSkeleton() {
    String moved =
        GOOD.replace(
            "<p>Basis. <span th:if=\"${f}\">Extra sentence.</span></p>",
            "<p>Basis. Extra sentence.<span th:if=\"${f}\"></span></p>");
    assertThat(moved).isNotEqualTo(GOOD);
    assertThat(violations(NL, moved)).anyMatch(v -> v.startsWith("skeleton differ in clause-a"));
    String movedIn =
        GOOD.replace(
            "<p>Basis. <span th:if=\"${f}\">Extra sentence.</span></p>",
            "<p><span th:if=\"${f}\">Basis. Extra sentence.</span></p>");
    assertThat(violations(NL, movedIn)).anyMatch(v -> v.startsWith("skeleton differ in clause-a"));
    assertThat(
            violations(
                NL,
                GOOD.replace("<strong th:text=\"${y}\">1</strong>", "<em th:text=\"${y}\">1</em>")))
        .anyMatch(v -> v.startsWith("skeleton differ in clause-a"));
  }

  @Test
  @DisplayName("checker fails when a number is dropped, invented or altered")
  void checkerFlagsNumbers() {
    assertThat(violations(NL, GOOD.replace("250", "205")))
        .anyMatch(v -> v.startsWith("numbers differ in clause-b"));
    assertThat(violations(NL, GOOD.replace("paragraph 2", "paragraph")))
        .anyMatch(v -> v.startsWith("numbers differ in clause-a"));
    assertThat(violations(NL, GOOD.replace("Amount of 250 euro.", "Amount of 250 euro in 2026.")))
        .anyMatch(v -> v.startsWith("numbers differ in clause-b"));
  }

  @Test
  @DisplayName("checker fails when a citation is altered, dropped or reordered")
  void checkerFlagsCitations() {
    assertThat(violations(NL, GOOD.replace("7:271 paragraph", "7:272 paragraph")))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    assertThat(violations(NL, GOOD.replace(" and 7:249", "")))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    assertThat(
            violations(
                NL, GOOD.replace("7:271 paragraph 2 and 7:249", "7:249 paragraph 2 and 7:271")))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }

  @Test
  @DisplayName("checker fails when the header is incomplete or its anchors are missing")
  void checkerFlagsHeader() {
    assertThat(
            violations(
                NL, GOOD.replace("translation: machine-drafted", "translation: authoritative")))
        .anyMatch(v -> v.startsWith("header rule"));
    assertThat(violations(NL, GOOD.replace("reviewed-by: none", "reviewed-by: counsel")))
        .contains("header rule: translation machine-drafted <=> reviewed-by none");
    assertThat(
            violations(
                NL,
                GOOD.replace("translation: machine-drafted", "translation: reviewed")
                    .replace("reviewed-by: none", "reviewed-by: counsel")))
        .noneMatch(v -> v.startsWith("header rule"));
    assertThat(violations(NL, GOOD.replace("translation: machine-drafted\n", "")))
        .contains("header marker missing: translation:");
    assertThat(violations(NL, GOOD.replace("reviewed-by: none\n", "")))
        .contains("header marker missing: reviewed-by:");
    assertThat(violations(NL, GOOD.replace("legal-basis:", "basis:")))
        .contains("header marker missing: legal-basis:");
    assertThat(violations(NL, GOOD.replace("2026-10-03", "2026-10-04")))
        .contains("header anchor missing: 2026-10-03");
    assertThat(violations(NL, GOOD.replace("wetten.overheid.nl", "example")))
        .contains("header anchor missing: wetten.overheid.nl");
    assertThat(violations(NL, GOOD.replace("01-07-2024", "01-07-2025")))
        .contains("header anchor missing: 01-07-2024");
    assertThat(violations(NL, GOOD.replace("art. 7:249", "art. 7:250")))
        .contains("header anchor missing: 7:249");
  }

  @Test
  @DisplayName("checker flags Dutch number words but allows the article 'een' and 'in acht nemen'")
  void checkerFlagsNumberWords() {
    String ok =
        "<div th:fragment=\"clause-a\"><p>Een afschrift binnen 14 dagen; neemt een opzegtermijn"
            + " in acht, 2 maal de huur.</p></div>";
    assertThat(numberWordViolations(ok)).isEmpty();
    for (String word :
        List.of(
            "veertien dagen",
            "tweemaal de huur",
            "eenmaal per jaar",
            "drie maanden",
            "acht weken")) {
      assertThat(
              numberWordViolations(
                  "<div th:fragment=\"clause-a\"><p>Binnen " + word + " betalen.</p></div>"))
          .as(word)
          .isNotEmpty();
    }
  }
}
