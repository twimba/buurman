package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Belgian citation pattern pairs the article number with the "§" paragraph digit in Dutch,
 * French ("§ 1er") and English alike, keeps Belgian slash and Latin suffixes in the token, and
 * makes the fidelity gate catch a swapped paragraph digit or a citation moved to another element.
 */
@DisplayName("BE lease documents: citation pattern for the fidelity gate")
class BeLeaseCitationPatternTest {

  private static final Optional<Pattern> BE = Optional.of(LeaseDocumentRegistry.BELGIAN_CITATION);

  private static String doc(String translation, String first, String second) {
    return """
    <!--
      legal-basis: Vlaams Woninghuurdecreet, codex.vlaanderen.be, verified 2026-10-04.
      reviewed-by: none
      translation: %s
    -->
    <div th:fragment="clause-a" data-clause="a">
      <p>%s</p>
      <p>%s</p>
    </div>
    """
        .formatted(translation, first, second);
  }

  private static final String NL =
      doc(
          "authoritative",
          "De waarborg bedraagt ten hoogste 3 maanden huur (artikel 37, § 1, van het Vlaams"
              + " Woninghuurdecreet).",
          "Zie artikel 224/1 en artikel 1728bis van het oud Burgerlijk Wetboek.");

  private static final String FR =
      doc(
          "machine-drafted",
          "La garantie ne peut excéder 3 mois de loyer (article 37, § 1er, du décret flamand sur la"
              + " location d'habitations).",
          "Voir l'article 224/1 et l'article 1728bis de l'ancien Code civil.");

  private static final String EN =
      doc(
          "machine-drafted",
          "The guarantee may not exceed 3 months' rent (article 37, § 1, of the Flemish Residential"
              + " Lease Decree).",
          "See article 224/1 and article 1728bis of the former Civil Code.");

  @Test
  @DisplayName("pairs the § digit in nl, fr (1er) and en, and keeps /n and bis in the token")
  void pairsParagraphInAllLanguages() {
    String first = LeaseDocumentText.fragments(NL).get("clause-a");
    assertThat(LeaseDocumentFidelityTest.citations(first, BE))
        .anyMatch(t -> t.endsWith(": 37 1"))
        .anyMatch(t -> t.endsWith(": 224/1"))
        .anyMatch(t -> t.endsWith(": 1728bis"));
    assertThat(
            LeaseDocumentFidelityTest.citations(
                LeaseDocumentText.fragments(FR).get("clause-a"), BE))
        .isEqualTo(LeaseDocumentFidelityTest.citations(first, BE));
    assertThat(
            LeaseDocumentFidelityTest.citations(
                LeaseDocumentText.fragments(EN).get("clause-a"), BE))
        .isEqualTo(LeaseDocumentFidelityTest.citations(first, BE));
  }

  @Test
  @DisplayName("accepts faithful fr and en translations")
  void acceptsFaithfulTranslations() {
    assertThat(LeaseDocumentFidelityTest.violations(NL, FR, BE)).isEmpty();
    assertThat(LeaseDocumentFidelityTest.violations(NL, EN, BE)).isEmpty();
  }

  @Test
  @DisplayName("flags a swapped paragraph digit and a changed article suffix")
  void flagsSwappedParagraphAndSuffix() {
    assertThat(LeaseDocumentFidelityTest.violations(NL, FR.replace("§ 1er", "§ 2"), BE))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    assertThat(LeaseDocumentFidelityTest.violations(NL, EN.replace("224/1", "224/2"), BE))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    assertThat(LeaseDocumentFidelityTest.violations(NL, EN.replace("1728bis", "1728ter"), BE))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }

  private static final String NL_LIST =
      doc(
          "authoritative",
          "Zie de artikelen 17 tot en met 19 van het Vlaams Woninghuurdecreet.",
          "Zie de artikelen 224 en 224/1 en de artikelen 237, § 5, en 239 van de Code.");

  private static final String FR_LIST =
      doc(
          "machine-drafted",
          "Voir les articles 17 à 19 du décret flamand sur la location d'habitations.",
          "Voir les articles 224 et 224/1 et les articles 237, § 5, et 239 du Code.");

  private static final String EN_LIST =
      doc(
          "machine-drafted",
          "See articles 17 to 19 of the Flemish Residential Lease Decree.",
          "See articles 224 and 224/1 and articles 237, § 5, and 239 of the Code.");

  @Test
  @DisplayName("the second number of a two-item article list is a token in nl, fr and en")
  void secondListNumberIsToken() {
    String nl = LeaseDocumentText.fragments(NL_LIST).get("clause-a");
    assertThat(LeaseDocumentFidelityTest.citations(nl, BE))
        .anyMatch(t -> t.endsWith(": 19"))
        .anyMatch(t -> t.endsWith(": 224/1"))
        .anyMatch(t -> t.endsWith(": 237 5"))
        .anyMatch(t -> t.endsWith(": 239"));
    assertThat(LeaseDocumentFidelityTest.violations(NL_LIST, FR_LIST, BE)).isEmpty();
    assertThat(LeaseDocumentFidelityTest.violations(NL_LIST, EN_LIST, BE)).isEmpty();
    assertThat(LeaseDocumentFidelityTest.violations(NL_LIST, FR_LIST.replace("à 19", "à 18"), BE))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    assertThat(
            LeaseDocumentFidelityTest.violations(
                NL_LIST, EN_LIST.replace("and 239", "and 238"), BE))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }
}
