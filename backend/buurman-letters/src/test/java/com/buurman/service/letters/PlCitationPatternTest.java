package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The Polish citation tokens keep letter and superscript suffixes of articles and paragraphs. */
@DisplayName("Polish citation pattern")
class PlCitationPatternTest {

  private static final Optional<Pattern> PL = Optional.of(LeaseDocumentRegistry.POLISH_CITATION);

  private static List<String> citations(String fragment) {
    return LeaseDocumentFidelityTest.citations(fragment, PL);
  }

  @Test
  @DisplayName("pl and en forms of the same references give the same tokens")
  void polishAndEnglishPair() {
    String pl =
        "<p>art. 688¹ § 1 k.c., art. 6a ust. 1 u.o.p.l., art. 8a ust. 4a albo ust. 4e u.o.p.l.,"
            + " art. 9 ust. 1b u.o.p.l. i art. 6 ust. 4 u.o.p.l.</p>";
    String en =
        "<p>Article 688¹ § 1 of the Civil Code, Article 6a, paragraph 1, Article 8a, paragraph 4a"
            + " or paragraph 4e, Article 9, paragraph 1b, and Article 6, paragraph 4, of the"
            + " Act</p>";
    assertThat(citations(pl))
        .containsExactlyInAnyOrder(
            "0: 688¹ 1", "0: 6a 1", "0: 8a", "0: 4a", "0: 4e", "0: 9", "0: 1b", "0: 6 4");
    assertThat(citations(en)).isEqualTo(citations(pl));
  }

  @Test
  @DisplayName("a changed letter or superscript suffix is caught")
  void suffixMutationsAreCaught() {
    assertThat(citations("<p>art. 6c u.o.p.l.</p>")).isNotEqualTo(citations("<p>art. 6b</p>"));
    assertThat(citations("<p>Article 688² of the Civil Code</p>"))
        .isNotEqualTo(citations("<p>art. 688¹ k.c.</p>"));
    assertThat(citations("<p>Article 8, paragraph 4</p>"))
        .isNotEqualTo(citations("<p>art. 8a ust. 4</p>"));
    assertThat(citations("<p>Article 9, paragraph 1</p>"))
        .isNotEqualTo(citations("<p>art. 9 ust. 1b</p>"));
  }
}
