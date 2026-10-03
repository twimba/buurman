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
 * structural attributes in the same order, the same element skeleton, the same statute citations
 * and bare numbers per fragment, and the legal header.
 *
 * <p>Rules of the checks, all language-neutral so that word order may change freely:
 *
 * <ul>
 *   <li>Skeleton: ordered tag names, plus for each element a flag "has text of its own" (non-blank
 *       text directly inside it, not inside a child). Words may move around an inline placeholder
 *       ({@code <strong th:text>}) but cannot move into or out of an element, e.g. a conditional
 *       span.
 *   <li>Citations: a citation token is the statute reference ({@code \d+:\d+[a-z]?}) plus the next
 *       bare number when it follows within 3 words in the same sentence of the same element (the
 *       "lid"/paragraph number: "7:271 lid 2", "7:271 Abs. 2", "7:271, al. 2"). Tokens are compared
 *       as a multiset per element (innermost enclosing {@code <p>}, {@code <li>}, conditional span
 *       ...), so citations may be reordered within an element but not moved to another one, and lid
 *       numbers cannot swap between citations.
 *   <li>Numbers: sorted multiset of the remaining digit sequences per fragment.
 * </ul>
 *
 * <p>Translator convention that makes the number check meaningful: every numeric quantity in the
 * prose (deadlines, periods, multiples, amounts, counts) is written as DIGITS exactly as in {@code
 * nl.html}, in every language, and never spelled out ("2 times", "2 fois", "2 Monatsmieten" for "2
 * maal de kale huurprijs", not "twice" or "zweifach"). {@code nl.html} is itself checked for Dutch
 * number words (whitespace tolerant): the article "een" is allowed ("een dag die valt", "een
 * termijn van 2 jaar") but "een" before maand/week/jaar, or after a quantity cue ("binnen een dag",
 * "per een termijn"), is a quantity; "in acht" is allowed only in the idiom "in acht nemen" (also
 * "in acht die ...", "... in acht." at the end of a clause), so "in acht weken" still flags.
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
  private static final Pattern IN_ACHT_IDIOM =
      Pattern.compile(
          "\\bin\\s+acht(?=\\s*[,.;:)!?]|\\s*$|\\s+(?:nemen|neemt|nam|genomen|neem|die|dat|en|of|te)\\b)",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern DUTCH_NUMBER_WORD =
      Pattern.compile(
          "\\b(twee|drie|vier|vijf|zes|zeven|acht|negen|tien|elf|twaalf|dertien|veertien|vijftien"
              + "|twintig|dertig|veertig|vijftig|zestig|zeventig|tachtig|negentig|honderd|duizend"
              + "|eenmaal|tweemaal|driemaal|viermaal|anderhalf|half|halve|dubbel\\w*"
              + "|een\\s+(?:maand|maanden|week|weken|jaar|jaren)"
              + "|(?:binnen|na|per|gedurende|elke|uiterlijk|ten\\s+minste|ten\\s+hoogste"
              + "|langer\\s+dan|korter\\s+dan|meer\\s+dan|minder\\s+dan)\\s+een\\s+(?:dag|termijn))\\b",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern SENTENCE_END = Pattern.compile("[()\\[\\];:]|[.!?](?=\\s+\\D)");
  private static final Pattern LID_AFTER_CITATION =
      Pattern.compile("^(?:[\\s,]+[^\\s\\d,]+){0,3}?[\\s,]+(?<![\\d:])(\\d+)(?![\\d:])");

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
              String prose = IN_ACHT_IDIOM.matcher(prose(body)).replaceAll(" ");
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

  private static final java.util.Set<String> VOID_TAGS =
      java.util.Set.of("br", "hr", "img", "input", "meta", "link", "wbr");

  /** One element of a fragment: its position, tag name and the text directly inside it. */
  private record Element(int index, String name, StringBuilder ownText) {}

  /** Open/close events of a fragment with each element's own text, in document order. */
  private static List<Element> elements(String fragment) {
    List<Element> all = new ArrayList<>();
    java.util.Deque<Element> stack = new java.util.ArrayDeque<>();
    Element root = new Element(-1, "#root", new StringBuilder());
    Matcher m = TAG.matcher(fragment);
    int last = 0;
    while (m.find()) {
      appendText(stack.isEmpty() ? root : stack.peek(), fragment.substring(last, m.start()));
      String name = m.group(2).toLowerCase();
      boolean closing = !m.group(1).isEmpty();
      boolean selfClosing = m.group().endsWith("/>") || VOID_TAGS.contains(name);
      if (closing) {
        if (!stack.isEmpty()) {
          stack.pop();
        }
      } else {
        Element element = new Element(all.size(), name, new StringBuilder());
        all.add(element);
        if (!selfClosing) {
          stack.push(element);
        }
      }
      last = m.end();
    }
    appendText(root, fragment.substring(last));
    all.add(0, root);
    return all;
  }

  private static void appendText(Element element, String text) {
    if (!text.isBlank()) {
      element.ownText().append(' ').append(text);
    }
  }

  /**
   * Language-neutral skeleton: ordered opening/closing tag names, and for each element a flag
   * whether it has non-blank text of its own. Words may move around an inline placeholder, but not
   * into or out of an element such as a conditional span.
   */
  static List<String> skeleton(String fragment) {
    List<String> result = new ArrayList<>();
    Matcher m = TAG.matcher(fragment);
    while (m.find()) {
      result.add(m.group(1) + m.group(2).toLowerCase());
    }
    for (Element element : elements(fragment)) {
      result.add(
          "#own-text " + element.index() + (element.ownText().length() > 0 ? " yes" : " no"));
    }
    return result;
  }

  private static String prose(String fragment) {
    return ENTITY.matcher(TAG.matcher(fragment).replaceAll(" ")).replaceAll(" ");
  }

  /**
   * Statute citation tokens, per element, as a sorted multiset: {@code index: 7:271 2} is the
   * reference with its lid number, {@code index: 7:249} one without.
   */
  static List<String> citations(String fragment) {
    List<String> result = new ArrayList<>();
    for (Element element : elements(fragment)) {
      String text = ENTITY.matcher(element.ownText()).replaceAll(" ");
      Matcher m = CITATION.matcher(text);
      while (m.find()) {
        String rest = text.substring(m.end());
        Matcher next = CITATION.matcher(rest);
        if (next.find()) {
          rest = rest.substring(0, next.start());
        }
        Matcher end = SENTENCE_END.matcher(rest);
        if (end.find()) {
          rest = rest.substring(0, end.start());
        }
        Matcher lid = LID_AFTER_CITATION.matcher(rest);
        result.add(element.index() + ": " + m.group() + (lid.find() ? " " + lid.group(1) : ""));
      }
    }
    Collections.sort(result);
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
  @DisplayName("checker fails when a citation is altered or dropped, or a lid number changes")
  void checkerFlagsCitations() {
    assertThat(violations(NL, GOOD.replace("7:271 paragraph", "7:272 paragraph")))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    assertThat(violations(NL, GOOD.replace(" and 7:249", "")))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    assertThat(violations(NL, GOOD.replace("paragraph 2", "paragraph 3")))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    // lid numbers swapped between two citations
    assertThat(
            violations(
                NL, GOOD.replace("7:271 paragraph 2 and 7:249", "7:249 paragraph 2 and 7:271")))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }

  @Test
  @DisplayName("citations are a multiset per element: reordering within one sentence is fine")
  void checkerAcceptsCitationReorderWithinElement() {
    assertThat(
            violations(
                NL, GOOD.replace("7:271 paragraph 2 and 7:249", "7:249 and 7:271 paragraph 2")))
        .isEmpty();
    // lid number may sit before or after filler words, with a comma
    assertThat(violations(NL, GOOD.replace("7:271 paragraph 2", "7:271, para. 2"))).isEmpty();
  }

  @Test
  @DisplayName("checker fails when a citation moves to another paragraph")
  void checkerFlagsCitationMovedBetweenElements() {
    String nl =
        "<div th:fragment=\"clause-a\"><p>Zie 7:271 lid 2 BW.</p><p>Zie 7:249 BW.</p></div>";
    String good = "<div th:fragment=\"clause-a\"><p>See 7:271 para. 2.</p><p>See 7:249.</p></div>";
    String moved = "<div th:fragment=\"clause-a\"><p>See 7:249.</p><p>See 7:271 para. 2.</p></div>";
    String header = "<!-- legal-basis: x reviewed-by: none translation: machine-drafted -->\n";
    String nlHeader = "<!-- legal-basis: x reviewed-by: none translation: authoritative -->\n";
    assertThat(violations(nlHeader + nl, header + good)).isEmpty();
    assertThat(violations(nlHeader + nl, header + moved))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }

  @Test
  @DisplayName("skeleton: words may move around an inline placeholder without moving out of it")
  void checkerAcceptsWordsAroundPlaceholder() {
    String nlHeader = "<!-- legal-basis: x reviewed-by: none translation: authoritative -->\n";
    String header = "<!-- legal-basis: x reviewed-by: none translation: machine-drafted -->\n";
    String nl =
        "<div th:fragment=\"clause-a\"><p><strong th:text=\"${a}\">verhuurder</strong>, hierna"
            + " de verhuurder.</p><p>Tot <strong th:text=\"${b}\">datum</strong></p></div>";
    String de =
        "<div th:fragment=\"clause-a\"><p>Der <strong th:text=\"${a}\">Vermieter</strong>,"
            + " nachfolgend Vermieter.</p><p><strong th:text=\"${b}\">Datum</strong>"
            + " einschlie\u00dflich</p></div>";
    assertThat(violations(nlHeader + nl, header + de)).isEmpty();
    String emptied =
        "<div th:fragment=\"clause-a\"><p><strong th:text=\"${a}\">Vermieter</strong></p>"
            + "<p>Bis <strong th:text=\"${b}\">Datum</strong></p></div>";
    assertThat(violations(nlHeader + nl, header + emptied))
        .anyMatch(v -> v.startsWith("skeleton differ in clause-a"));
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
            + " in acht, 2 maal de huur; neemt de regels in\n acht. Opzeggen tegen een dag die valt"
            + " voor het einde, voor een termijn van 2 jaar. Zij dienen de termijn in acht te"
            + " nemen.</p></div>";
    assertThat(numberWordViolations(ok)).isEmpty();
    for (String word :
        List.of(
            "veertien dagen",
            "tweemaal de huur",
            "eenmaal per jaar",
            "drie maanden",
            "acht weken",
            "in acht weken",
            "in acht dagen",
            "in acht maanden",
            "in acht jaar",
            "in\n acht weken",
            "twee\njaar",
            "een\n maand",
            "anderhalf jaar",
            "zeventig dagen",
            "tachtig euro",
            "viermaal per jaar",
            "driemaal de huur",
            "dubbele huur",
            "het dubbele",
            "binnen een dag",
            "na een dag",
            "binnen een termijn",
            "ten minste een termijn")) {
      assertThat(
              numberWordViolations(
                  "<div th:fragment=\"clause-a\"><p>Binnen " + word + " betalen.</p></div>"))
          .as(word)
          .isNotEmpty();
    }
  }
}
