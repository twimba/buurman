package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The FI citation pattern of {@link LeaseDocumentRegistry#FINNISH_CITATION} pairs a section with
 * its subsection alike in Finnish ("52 § 2 mom."), Swedish ("52 § 2 mom.") and English ("section
 * 52, subsection 2"), and the fidelity gate catches a swapped subsection digit.
 */
@DisplayName("Finnish citation pattern")
class FinnishCitationPatternTest {

  private static final Optional<Pattern> FI = Optional.of(LeaseDocumentRegistry.FINNISH_CITATION);

  private static List<String> citations(String fragment) {
    return LeaseDocumentFidelityTest.citations(fragment, FI);
  }

  @Test
  @DisplayName("Finnish, Swedish and English forms give the same section and subsection pairs")
  void threeLanguagesPairAlike() {
    List<String> fi =
        citations(
            "<p>Irtisanomisaika on 3 kuukautta (AHVL 52 § 2 mom.). Ilmoitus annetaan"
                + " todisteellisesti (AHVL 13 a § 1 mom.), ks. myös (AHVL 62 §).</p>");
    List<String> sv =
        citations(
            "<p>Uppsägningstiden är 3 månader (AHVL 52 § 2 mom.). Meddelandet delges"
                + " bevisligen (AHVL 13 a § 1 mom.), se även (AHVL 62 §).</p>");
    List<String> en =
        citations(
            "<p>The notice period is 3 months (AHVL section 52, subsection 2). The notice is"
                + " served verifiably (AHVL section 13 a, subsection 1), see also (AHVL section"
                + " 62).</p>");
    assertThat(fi).containsExactlyInAnyOrder("0: 52 2", "0: 13a 1", "0: 62");
    assertThat(sv).isEqualTo(fi);
    assertThat(en).isEqualTo(fi);
  }

  @Test
  @DisplayName("a subsection that is a point list keeps the point digit out of the pair")
  void pointDigitIsNotPaired() {
    assertThat(citations("<p>(AHVL 61 § 1 mom. 1 kohta)</p>"))
        .isEqualTo(citations("<p>(AHVL section 61, subsection 1, paragraph 1)</p>"))
        .containsExactly("0: 61 1");
  }

  @Test
  @DisplayName("an act number before the section is not a citation")
  void actNumberIsNotACitation() {
    assertThat(citations("<p>(laki 50/2013, 6 § 2 mom.)</p>"))
        .isEqualTo(citations("<p>(Act 50/2013, section 6, subsection 2)</p>"))
        .containsExactly("0: 6 2");
  }

  @Test
  @DisplayName("a swapped subsection digit in a translation is caught")
  void swappedSubsectionIsCaught() {
    assertThat(citations("<p>(AHVL 52 § 2 mom.) and (AHVL 54 § 1 mom.)</p>"))
        .isNotEqualTo(
            citations(
                "<p>(AHVL section 52, subsection 1) and (AHVL section 54, subsection 2)</p>"));
  }
}
