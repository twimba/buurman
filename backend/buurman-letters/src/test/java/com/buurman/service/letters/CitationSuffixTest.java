package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Letter, word and dotted suffixes of statute numbers belong to the citation token where the
 * documents use them, so a changed suffix in a translation fails the fidelity gate.
 */
@DisplayName("citation suffixes are part of the token")
class CitationSuffixTest {

  private static String doc(String translation, String body) {
    return "<!-- legal-basis: x reviewed-by: none translation: "
        + translation
        + " -->\n<div th:fragment=\"clause-a\"><p>"
        + body
        + "</p></div>";
  }

  private static void assertSuffixGuarded(
      Pattern pattern, String authoritative, String translation, String suffix, String changed) {
    Optional<Pattern> p = Optional.of(pattern);
    String auth = doc("authoritative", authoritative);
    String good = doc("machine-drafted", translation);
    assertThat(LeaseDocumentFidelityTest.violations(auth, good, p)).as("faithful").isEmpty();
    assertThat(translation).contains(suffix);
    assertThat(LeaseDocumentFidelityTest.violations(auth, good.replace(suffix, changed), p))
        .as("changed suffix %s -> %s", suffix, changed)
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }

  @Test
  @DisplayName("FR: the spaced capital of 'article 261 D' is part of the token")
  void french() {
    assertSuffixGuarded(
        LeaseDocumentRegistry.FRENCH_CITATION,
        "Le loyer est exonéré de TVA (article 261 D du code général des impôts).",
        "The rent is exempt from VAT (article 261 D of the General Tax Code).",
        "261 D",
        "261 E");
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>article 22 à 25, article L. 145-38-1, article 5 a lease</p>",
                Optional.of(LeaseDocumentRegistry.FRENCH_CITATION)))
        .containsExactlyInAnyOrder("0: 22 25", "0: L.145-38-1", "0: 5");
  }

  @Test
  @DisplayName("ES: dotted and word parts of 'artículo 52.1.7.º' and '20.Uno.23.º' are kept")
  void spanish() {
    assertSuffixGuarded(
        LeaseDocumentRegistry.SPANISH_CITATION,
        "Es competente el juez del lugar de la finca (artículo 52.1.7.º de la Ley de"
            + " Enjuiciamiento Civil).",
        "The court of the place of the property has jurisdiction (article 52.1.7.º of the Civil"
            + " Procedure Act).",
        "52.1.7",
        "52.1.8");
    assertSuffixGuarded(
        LeaseDocumentRegistry.SPANISH_CITATION,
        "Exento según el artículo 20.Uno.23.º de la Ley del IVA.",
        "Exempt under article 20.Uno.23.º of the VAT Act.",
        "20.Uno.23",
        "20.Dos.23");
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>artículo 17.7, artículo 9. El</p>",
                Optional.of(LeaseDocumentRegistry.SPANISH_CITATION)))
        .containsExactlyInAnyOrder("0: 17.7", "0: 9");
  }

  @Test
  @DisplayName("PT: the letter of 'artigo 1110.º-A' / 'article 1110-A' is a token of its own")
  void portuguese() {
    assertSuffixGuarded(
        LeaseDocumentRegistry.PORTUGUESE_CITATION,
        "Aplica-se o artigo 1110.º-A, n.º 1, do Código Civil.",
        "Article 1110-A, paragraph 1, of the Civil Code applies.",
        "1110-A",
        "1110-B");
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>artigo 1110.º-A, n.º 1</p>",
                Optional.of(LeaseDocumentRegistry.PORTUGUESE_CITATION)))
        .containsExactlyInAnyOrder("0: 1110 1", "0: -A 1");
  }

  @Test
  @DisplayName("NO: the letter of '§ 9-3 a' is part of the token, a following word is not")
  void norwegian() {
    assertSuffixGuarded(
        LeaseDocumentRegistry.NORWEGIAN_CITATION,
        "Minstetid ned til 1 år (§ 9-3 a).",
        "A minimum term as short as 1 year (§ 9-3 a).",
        "§ 9-3 a",
        "§ 9-3 b");
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>§ 9-3 i husleieloven, § 9-3 a landlord, § 9-3 2. ledd</p>",
                Optional.of(LeaseDocumentRegistry.NORWEGIAN_CITATION)))
        .containsExactlyInAnyOrder("0: §9-3", "0: §9-3", "0: §9-3 2");
  }

  @Test
  @DisplayName("LU: the Latin suffix of 'paragraphe 2bis' is part of a token")
  void luxembourg() {
    assertSuffixGuarded(
        LeaseDocumentRegistry.LUXEMBOURG_CITATION,
        "La garantie est restituée (article 5, paragraphe 2bis, de la loi modifiée).",
        "The guarantee is returned (article 5, paragraph 2bis, of the amended Law).",
        "2bis",
        "2ter");
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>Artikel 12 Absatz 3 und Artikel 5 Absatz 2bis</p>",
                Optional.of(LeaseDocumentRegistry.LUXEMBOURG_CITATION)))
        .containsExactlyInAnyOrder("0: 12 3", "0: 5", "0: 2bis");
  }

  @Test
  @DisplayName("DE: the letter of 'Abs. 1a' / 'para. 1a' is part of a token")
  void german() {
    assertSuffixGuarded(
        LeaseDocumentRegistry.GERMAN_CITATION,
        "Zu viel gezahlte Miete ist zurückzuzahlen (§ 556g Abs. 1a BGB).",
        "Rent paid in excess must be repaid (§ 556g para. 1a BGB).",
        "1a",
        "1b");
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>§ 556 Abs. 2 und § 556g Abs. 1a</p>",
                Optional.of(LeaseDocumentRegistry.GERMAN_CITATION)))
        .containsExactlyInAnyOrder("0: §556 2", "0: §556g", "0: 1a");
  }
}
