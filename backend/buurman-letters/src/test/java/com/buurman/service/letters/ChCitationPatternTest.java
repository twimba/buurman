package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Swiss citation pattern of the CH registry entries pairs the article number, letter suffix
 * included, with the Abs./al./cpv./paragraph digit in all 4 languages, and the fidelity gate bites
 * on a changed paragraph or a dropped suffix.
 */
@DisplayName("CH citation pattern")
class ChCitationPatternTest {

  private static final Optional<Pattern> CH = Optional.of(LeaseDocumentRegistry.SWISS_CITATION);

  private static final String DE =
      "<!-- legal-basis: OR x reviewed-by: none translation: authoritative -->\n"
          + "<div th:fragment=\"clause-a\"><p>Die Kündigung ist anfechtbar (Art. 271 Abs. 1 OR,"
          + " Art. 271a Abs. 3 Bst. a OR).</p><p>Höchstens 3 Monatszinse (Art. 257e Abs. 2"
          + " OR).</p></div>";

  private static String translation(String body) {
    return "<!-- legal-basis: OR x reviewed-by: none translation: machine-drafted -->\n"
        + "<div th:fragment=\"clause-a\">"
        + body
        + "</div>";
  }

  @Test
  @DisplayName("de, fr, it and en pair the article with its paragraph digit, suffix included")
  void pairsInAllLanguages() {
    for (String text :
        List.of(
            "<p>(Art. 271 Abs. 1 OR, Art. 271a Abs. 3 Bst. a OR)</p>",
            "<p>(art. 271, al. 1, CO, art. 271a, al. 3, let. a, CO)</p>",
            "<p>(art. 271 cpv. 1 CO, art. 271a cpv. 3 lett. a CO)</p>",
            "<p>(Article 271 paragraph 1 CO, Article 271a paragraph 3 letter a CO)</p>")) {
      assertThat(LeaseDocumentFidelityTest.citations(text, CH))
          .as(text)
          .hasSize(2)
          .anyMatch(c -> c.endsWith(": 271 1"))
          .anyMatch(c -> c.endsWith(": 271a 3"));
    }
    assertThat(LeaseDocumentFidelityTest.citations("<p>dall'art. 266l CO</p>", CH))
        .containsExactly("0: 266l");
  }

  @Test
  @DisplayName("faithful translations pass; a changed paragraph or a dropped suffix fails")
  void gateBites() {
    String fr =
        translation(
            "<p>Le congé est annulable (art. 271, al. 1, CO, art. 271a, al. 3, let. a, CO).</p>"
                + "<p>Au plus 3 mois de loyer (art. 257e, al. 2, CO).</p>");
    String en =
        translation(
            "<p>The termination may be challenged (Article 271 paragraph 1 CO, Article 271a"
                + " paragraph 3 letter a CO).</p><p>At most 3 months' rent (Article 257e paragraph"
                + " 2 CO).</p>");
    assertThat(LeaseDocumentFidelityTest.violations(DE, fr, CH)).isEmpty();
    assertThat(LeaseDocumentFidelityTest.violations(DE, en, CH)).isEmpty();
    assertThat(
            LeaseDocumentFidelityTest.violations(DE, en.replace("paragraph 1", "paragraph 2"), CH))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    assertThat(LeaseDocumentFidelityTest.violations(DE, fr.replace("art. 257e", "art. 257"), CH))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }
}
