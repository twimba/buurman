package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Mechanical fidelity gate: a translated lease document may change prose only. Against the
 * authoritative document of its (country, kind) in the {@link LeaseDocumentRegistry} (for NL
 * residential {@code nl.html}) it must keep the same fragments, the same Thymeleaf expressions and
 * structural attributes in the same order, the same element skeleton, the same statute citations
 * and bare numbers, and the legal header.
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
 *       numbers cannot swap between citations. Non-digit characters glued to the reference are
 *       skipped before the words are counted (PT "artigo 1097.º, n.º 3": the ordinal sign ".º").
 *   <li>Numbers: sorted multiset of the remaining digit sequences, keyed by the element holding
 *       them, so numbers may be reordered within an element but not swapped across paragraphs.
 * </ul>
 *
 * <p>Citation convention for translators (all languages): write the paragraph number as a DIGIT
 * after the statute citation, within the same sentence or parenthesis: "7:271 lid 2" is fr "art.
 * 7:271, alinéa 2", de "7:271 Abs. 2", sv "7:271 st. 2", da "7:271, stk. 2", nb "7:271 2. ledd", fi
 * "7:271 2 momentti", pl "7:271 ust. 2", el "7:271 παρ. 2". Never put the paragraph before the
 * article ("Absatz 2 von 7:271", "l'alinéa 2 de l'article 7:271") and never spell ordinals out
 * ("andra stycket").
 *
 * <p>Inherent limits: the gate checks structure, expressions, citations and digits only. A deleted
 * or added sentence without digits, citation or expression, a dropped "niet" or "schriftelijk", or
 * a huurder/verhuurder swap is NOT detectable; human and legal review covers those.
 *
 * <p>Translator convention that makes the number check meaningful: every numeric quantity in the
 * prose (deadlines, periods, multiples, amounts, counts) is written as DIGITS exactly as in {@code
 * nl.html}, in every language, and never spelled out ("2 maal de kale huurprijs" is "2 times the
 * net rent", "2 Kaltmieten", "2 fois le loyer de base", never "twice" or "zweifach"). {@code
 * nl.html} is itself checked for Dutch number words (whitespace tolerant), as is the authoritative
 * document of every registry entry in its own language ({@link LeaseNumberWords}, one rule set per
 * language). Not quantities in this sense, and therefore allowed spelled out in the non-Dutch
 * lints: the count of contracting parties ("the two parties", fr "les deux parties", es "las dos
 * partes", ...) and a "calendar year" noun where it only names a period boundary ("after the end of
 * a calendar year"); see {@link LeaseNumberWords} for the exact exemptions. For Dutch: the article
 * "een" is allowed ("een dag die valt", "een termijn van 2 jaar") but "een" before maand/week/jaar,
 * or after a quantity cue ("binnen een dag", "per een termijn"), is a quantity; "in acht" is
 * allowed only in the idiom "in acht nemen" (also "in acht die ...", "... in acht." at the end of a
 * clause), so "in acht weken" still flags.
 */
@DisplayName("lease document fidelity")
class LeaseDocumentFidelityTest {

  private static final String DOCUMENT_ROOT = "templates/documents/lease-agreement/";

  private static final Set<String> VOID_TAGS =
      Set.of("br", "hr", "img", "input", "meta", "link", "wbr");
  private static final Pattern STRUCTURE =
      Pattern.compile("</?th:[\\w-]+|(?:th:[\\w-]+|data-[\\w-]+)=\"[^\"]*\"");
  private static final Pattern TAG =
      Pattern.compile("<(/?)([a-zA-Z][\\w:-]*)(?:\"[^\"]*\"|'[^']*'|[^>\"'])*>");
  private static final Pattern ENTITY = Pattern.compile("&[#a-zA-Z0-9]+;");
  private static final Pattern NUMBER = Pattern.compile("\\d+");
  private static final Pattern HEADER_ANCHOR =
      Pattern.compile(
          "\\d{4}-\\d{2}-\\d{2}|\\d{2}-\\d{2}-\\d{4}|\\d{2}\\.\\d{2}\\.\\d{4}|\\d{2}/\\d{2}/\\d{4}"
              + "|\\d+:\\d+[a-z]?|§\\s*\\d+[a-z]?"
              + "|[a-z][a-z0-9-]*(?:\\.[a-z0-9-]+)*\\.(?:nl|eu|de|fr|es|pt|it|se|dk|no|fi|gr|pl|at"
              + "|be|lu|ie|cz|ch|ca|us|uk|gov|int)\\b|BWBR\\d+|CELEX\\s+\\w+");
  private static final Pattern SENTENCE_END = Pattern.compile("[()\\[\\];:]|[.!?](?=\\s+\\D)");
  private static final Pattern LID_AFTER_CITATION =
      Pattern.compile("^[^\\s\\d,]*(?:[\\s,]+[^\\s\\d,]+){0,3}?[\\s,]+(?<![\\d:])(\\d+)(?![\\d:])");

  /**
   * All violations of {@code other} against the {@code authoritative} document, with the Dutch
   * citation format.
   */
  static List<String> violations(String authoritative, String other) {
    return violations(authoritative, other, Optional.of(LeaseDocumentRegistry.DUTCH_CITATION));
  }

  /**
   * All violations of {@code other} against the {@code authoritative} document. {@code citation} is
   * the entry's statute-reference format ({@link LeaseDocumentRegistry.Entry#citationPattern});
   * when absent no citation pairing is done and every digit sequence is compared per element.
   */
  static List<String> violations(String authoritative, String other, Optional<Pattern> citation) {
    List<String> problems = new ArrayList<>(headerViolations(authoritative, other));

    Map<String, String> nlFragments = fragments(authoritative);
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
          compare(
              problems,
              "citations",
              name,
              citations(nlBody, citation),
              citations(otherBody, citation));
          compare(
              problems, "numbers", name, numbers(nlBody, citation), numbers(otherBody, citation));
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
    return numberWordViolations(nl, "nl");
  }

  /** Spelled-out numbers left in the prose of an authoritative document in {@code language}. */
  static List<String> numberWordViolations(String authoritative, String language) {
    return LeaseNumberWords.violations(authoritative, language);
  }

  private static List<String> headerViolations(String authoritative, String other) {
    String header = header(other);
    List<String> problems =
        new ArrayList<>(LeaseDocumentText.headerPolicyViolations(header, false));
    Matcher m = HEADER_ANCHOR.matcher(legalBasis(header(authoritative)));
    while (m.find()) {
      if (!header.contains(m.group())) {
        problems.add("header anchor missing: " + m.group());
      }
    }
    return problems;
  }

  private static String header(String html) {
    return LeaseDocumentText.header(html);
  }

  /** The legal-basis block: from its marker up to the reviewed-by marker. */
  private static String legalBasis(String header) {
    int start = header.indexOf("legal-basis:");
    int end = header.indexOf("reviewed-by:");
    return start < 0 ? "" : header.substring(start, end < start ? header.length() : end);
  }

  /** Fragment name to the markup from its opening tag up to the next fragment's opening tag. */
  static Map<String, String> fragments(String html) {
    return LeaseDocumentText.fragments(html);
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

  /** One element of a fragment: its position, tag name and the text directly inside it. */
  private record Element(int index, String name, StringBuilder ownText) {}

  /** Open/close events of a fragment with each element's own text, in document order. */
  private static List<Element> elements(String fragment) {
    List<Element> all = new ArrayList<>();
    Deque<Element> stack = new ArrayDeque<>();
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

  static String prose(String fragment) {
    return LeaseDocumentText.prose(fragment);
  }

  /**
   * Statute citation tokens, per element, as a sorted multiset: {@code index: 7:271 2} is the
   * reference with its lid number, {@code index: 7:249} one without.
   */
  static List<String> citations(String fragment) {
    return citations(fragment, Optional.of(LeaseDocumentRegistry.DUTCH_CITATION));
  }

  /**
   * As {@link #citations(String)} for a country's citation format; empty (no pairing) when the
   * format is absent. Whitespace inside a token is dropped so "§ 556" equals "§556".
   */
  static List<String> citations(String fragment, Optional<Pattern> citation) {
    List<String> result = new ArrayList<>();
    if (citation.isEmpty()) {
      return result;
    }
    Pattern pattern = citation.get();
    for (Element element : elements(fragment)) {
      String text = ENTITY.matcher(element.ownText()).replaceAll(" ");
      Matcher m = pattern.matcher(text);
      while (m.find()) {
        String rest = text.substring(m.end());
        Matcher next = pattern.matcher(rest);
        if (next.find()) {
          rest = rest.substring(0, next.start());
        }
        Matcher end = SENTENCE_END.matcher(rest);
        if (end.find()) {
          rest = rest.substring(0, end.start());
        }
        Matcher lid = LID_AFTER_CITATION.matcher(rest);
        result.add(
            element.index()
                + ": "
                + m.group().replaceAll("\\s+", "")
                + (lid.find() ? " " + lid.group(1) : ""));
      }
    }
    Collections.sort(result);
    return result;
  }

  /** Sorted multiset of the remaining digit sequences, keyed by the element holding them. */
  static List<String> numbers(String fragment) {
    return numbers(fragment, Optional.of(LeaseDocumentRegistry.DUTCH_CITATION));
  }

  /** As {@link #numbers(String)}; citation tokens are excluded only when a format is given. */
  static List<String> numbers(String fragment, Optional<Pattern> citation) {
    List<String> result = new ArrayList<>();
    for (Element element : elements(fragment)) {
      String text = ENTITY.matcher(element.ownText()).replaceAll(" ");
      String withoutCitations = citation.map(p -> p.matcher(text).replaceAll(" ")).orElse(text);
      Matcher m = NUMBER.matcher(withoutCitations);
      while (m.find()) {
        result.add(element.index() + ": " + m.group());
      }
    }
    Collections.sort(result);
    return result;
  }

  private static String read(LeaseDocumentRegistry.Entry entry, String language)
      throws IOException {
    try (var in =
        new ClassPathResource(DOCUMENT_ROOT + entry.key() + "/" + language + ".html")
            .getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  @DisplayName(
      "every enforced translation is faithful to the authoritative document of its (country, kind)")
  void translationsAreFaithful() throws IOException {
    for (LeaseDocumentRegistry.Entry entry : LeaseDocumentRegistry.ENTRIES) {
      String authoritative = read(entry, entry.authoritativeLanguage());
      for (String language : entry.translations()) {
        assertThat(violations(authoritative, read(entry, language), entry.citationPattern()))
            .as("%s/%s fidelity against %s", entry.key(), language, entry.authoritativeLanguage())
            .isEmpty();
      }
    }
  }

  @Test
  @DisplayName(
      "the authoritative document is marked authoritative and writes every quantity as digits")
  void authoritativeIsAuthoritativeAndDigitOnly() throws IOException {
    for (LeaseDocumentRegistry.Entry entry : LeaseDocumentRegistry.ENTRIES) {
      String authoritative = read(entry, entry.authoritativeLanguage());
      assertThat(LeaseDocumentText.headerPolicyViolations(header(authoritative), true))
          .as("%s/%s header", entry.key(), entry.authoritativeLanguage())
          .isEmpty();
      assertThat(numberWordViolations(authoritative, entry.authoritativeLanguage()))
          .as("%s/%s number words", entry.key(), entry.authoritativeLanguage())
          .isEmpty();
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
  @DisplayName("numbers are a multiset per element: swapping across paragraphs fails")
  void checkerFlagsNumbersSwappedBetweenElements() {
    String nlHeader = "<!-- legal-basis: x reviewed-by: none translation: authoritative -->\n";
    String header = "<!-- legal-basis: x reviewed-by: none translation: machine-drafted -->\n";
    String nl =
        "<div th:fragment=\"clause-a\"><p>Binnen 14 dagen.</p><p>Binnen 30 dagen of 2"
            + " maal.</p></div>";
    String good =
        "<div th:fragment=\"clause-a\"><p>Within 14 days.</p><p>Within 2 times or 30"
            + " days.</p></div>";
    String swapped =
        "<div th:fragment=\"clause-a\"><p>Within 30 days.</p><p>Within 14 times or 2"
            + " days.</p></div>";
    assertThat(violations(nlHeader + nl, header + good)).isEmpty();
    assertThat(violations(nlHeader + nl, header + swapped))
        .anyMatch(v -> v.startsWith("numbers differ in clause-a"));
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
        .as("strict policy: a translation is machine-drafted with reviewed-by none")
        .anyMatch(v -> v.startsWith("header rule"));
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

  // ---- citation pattern of a non-Dutch country (the registry supplies it per entry) ----

  private static final Optional<Pattern> DE_CITATION =
      Optional.of(Pattern.compile("§\\s*\\d+[a-z]?"));

  private static final String DE_AUTH =
      "<!-- legal-basis: BGB x reviewed-by: none translation: authoritative -->\n"
          + "<div th:fragment=\"clause-a\"><p>Nach § 556 Abs. 2 und § 557 Abs. 3 BGB.</p>"
          + "<p>Siehe § 558a.</p></div>";

  private static String deTranslation(String body) {
    return "<!-- legal-basis: BGB x reviewed-by: none translation: machine-drafted -->\n"
        + "<div th:fragment=\"clause-a\">"
        + body
        + "</div>";
  }

  @Test
  @DisplayName("a DE-style citation pattern pairs a section with its paragraph digit")
  void customCitationPatternPairsParagraphs() {
    String good =
        deTranslation("<p>Under § 556 para. 2 and § 557 para. 3 BGB.</p><p>See § 558a.</p>");
    assertThat(violations(DE_AUTH, good, DE_CITATION)).isEmpty();
    assertThat(citations("<p>Nach § 556 Abs. 2 und § 557 Abs. 3</p>", DE_CITATION))
        .hasSize(2)
        .anyMatch(c -> c.endsWith("§556 2"))
        .anyMatch(c -> c.endsWith("§557 3"));
  }

  @Test
  @DisplayName("paragraph digits swapped between two citations of one sentence are caught")
  void customCitationPatternCatchesSwappedParagraphs() {
    String swapped =
        deTranslation("<p>Under § 556 para. 3 and § 557 para. 2 BGB.</p><p>See § 558a.</p>");
    assertThat(violations(DE_AUTH, swapped, DE_CITATION))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }

  @Test
  @DisplayName("reordering two citations within one sentence is allowed, moving one is not")
  void customCitationPatternAllowsReordering() {
    String reordered =
        deTranslation("<p>Under § 557 para. 3 and § 556 para. 2 BGB.</p><p>See § 558a.</p>");
    assertThat(violations(DE_AUTH, reordered, DE_CITATION)).isEmpty();
    String moved =
        deTranslation("<p>Under § 556 para. 2 BGB.</p><p>See § 557 para. 3 and § 558a.</p>");
    assertThat(violations(DE_AUTH, moved, DE_CITATION))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }

  @Test
  @DisplayName("without a citation pattern the digits are still compared per element")
  void withoutCitationPatternDigitsStillCompared() {
    String swapped =
        deTranslation("<p>Under § 556 para. 3 and § 557 para. 2 BGB.</p><p>See § 558a.</p>");
    // same multiset of digits in the same element: reordered digits cannot be told apart
    assertThat(violations(DE_AUTH, swapped, Optional.empty())).isEmpty();
    String changed =
        deTranslation("<p>Under § 556 para. 2 and § 557 para. 4 BGB.</p><p>See § 558a.</p>");
    assertThat(violations(DE_AUTH, changed, Optional.empty()))
        .anyMatch(v -> v.startsWith("numbers differ in clause-a"));
  }

  // ---- PT: the ordinal sign glued to the article number ("1097.º, n.º 3") ----

  private static final Optional<Pattern> PT_CITATION =
      Optional.of(LeaseDocumentRegistry.PORTUGUESE_CITATION);

  private static final String PT_AUTH =
      "<!-- legal-basis: CC x reviewed-by: none translation: authoritative -->\n"
          + "<div th:fragment=\"clause-a\"><p>Nos termos do artigo 1097.º, n.º 3, e do artigo"
          + " 1110.º-A, n.º 1, do Código Civil.</p><p>Ver o artigo 1083.º do Código"
          + " Civil.</p></div>";

  @Test
  @DisplayName("a PT citation pairs the article with the n.º paragraph after the ordinal sign")
  void portugueseCitationPairsParagraphAfterOrdinalSign() {
    assertThat(citations("<p>artigo 1097.º, n.º 3, e artigo 1110.º-A, n.º 1</p>", PT_CITATION))
        .hasSize(2)
        .anyMatch(c -> c.endsWith(": 1097 3"))
        .anyMatch(c -> c.endsWith(": 1110 1"));
    assertThat(
            citations(
                "<p>article 1097, paragraph 3, and article 1110-A, paragraph 1</p>", PT_CITATION))
        .anyMatch(c -> c.endsWith(": 1097 3"))
        .anyMatch(c -> c.endsWith(": 1110 1"));
    String good =
        deTranslation(
            "<p>Under article 1097, paragraph 3, and article 1110-A, paragraph 1, of the Civil"
                + " Code.</p><p>See article 1083 of the Civil Code.</p>");
    assertThat(violations(PT_AUTH, good, PT_CITATION)).isEmpty();
    String swapped =
        deTranslation(
            "<p>Under article 1097, paragraph 1, and article 1110-A, paragraph 3, of the Civil"
                + " Code.</p><p>See article 1083 of the Civil Code.</p>");
    assertThat(violations(PT_AUTH, swapped, PT_CITATION))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }
}
