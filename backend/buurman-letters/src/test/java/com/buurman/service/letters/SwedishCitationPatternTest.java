package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The SE citation pattern of {@link LeaseDocumentRegistry#SWEDISH_CITATION} pairs a section with
 * its paragraph (stycke) alike in Swedish ("12 kap. 46 § 1 st. JB") and English ("Land Code,
 * Chapter 12, section 46, paragraph 1"), and the fidelity gate catches a swapped paragraph digit.
 */
@DisplayName("Swedish citation pattern")
class SwedishCitationPatternTest {

  private static final Optional<Pattern> SE = Optional.of(LeaseDocumentRegistry.SWEDISH_CITATION);

  private static List<String> citations(String fragment) {
    return LeaseDocumentFidelityTest.citations(fragment, SE);
  }

  private static List<String> numbers(String fragment) {
    return LeaseDocumentFidelityTest.numbers(fragment, SE);
  }

  @Test
  @DisplayName("Swedish and English forms give the same section and paragraph pairs")
  void bothLanguagesPairAlike() {
    String sv =
        "<p>Uppsägningstiden är 3 månader (12 kap. 4 § 1 st. 1 p. JB). Hyresvärden ska anslå"
            + " uppgifterna (12 kap. 18 i § JB), se även (12 kap. 46 § 1 st. JB).</p>";
    String en =
        "<p>The notice period is 3 months (Land Code, Chapter 12, section 4, paragraph 1, point 1)."
            + " The landlord posts the details (Land Code, Chapter 12, section 18 i), see also"
            + " (Land Code, Chapter 12, section 46, paragraph 1).</p>";
    assertThat(citations(sv)).containsExactlyInAnyOrder("0: 4 1", "0: 18i", "0: 46 1");
    assertThat(citations(en)).isEqualTo(citations(sv));
    assertThat(numbers(en)).isEqualTo(numbers(sv));
  }

  @Test
  @DisplayName("an act number before the section is not a citation")
  void actNumberIsNotACitation() {
    String sv = "<p>(5 § 1 st. 3 p. lagen (2006:985) om energideklaration för byggnader)</p>";
    String en =
        "<p>(section 5, paragraph 1, point 3, of the Energy Performance of Buildings Act"
            + " (2006:985))</p>";
    assertThat(citations(sv)).isEqualTo(citations(en)).containsExactly("0: 5 1");
    assertThat(numbers(sv)).isEqualTo(numbers(en));
  }

  @Test
  @DisplayName("sections joined by 'och' / 'and' each keep their own token without a paragraph")
  void joinedSectionsStayApart() {
    assertThat(citations("<p>(12 kap. 33 §, 34 § och 35 § JB)</p>"))
        .isEqualTo(
            citations("<p>(Land Code, Chapter 12, section 33, section 34 and section 35)</p>"))
        .containsExactlyInAnyOrder("0: 33", "0: 34", "0: 35");
  }

  @Test
  @DisplayName("a swapped paragraph digit in a translation is caught")
  void swappedParagraphIsCaught() {
    assertThat(citations("<p>(12 kap. 58 § 1 st. JB) och (12 kap. 58 a § 2 st. JB)</p>"))
        .isNotEqualTo(
            citations(
                "<p>(Land Code, Chapter 12, section 58, paragraph 2) and (Land Code, Chapter 12,"
                    + " section 58 a, paragraph 1)</p>"));
  }
}
