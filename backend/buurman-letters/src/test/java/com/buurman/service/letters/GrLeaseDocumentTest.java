package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.LeaseKind;

/**
 * Greek specifics of the lease gates: the citation pattern pairs "άρθρο 13 παρ. 1 του ν. 4242/2014"
 * with "article 13, paragraph 1, of Law 4242/2014", and the Greek marker words of the GR entries
 * are found next to Greek letters (the documents themselves are checked by {@code
 * LeaseDocumentRenderTest}).
 */
@DisplayName("GR lease documents")
class GrLeaseDocumentTest {

  private static final Optional<Pattern> GR = Optional.of(LeaseDocumentRegistry.GREEK_CITATION);

  private static final String AUTH =
      "<!-- legal-basis: AK x reviewed-by: none translation: authoritative -->\n"
          + "<div th:fragment=\"clause-a\"><p>Η μίσθωση ισχύει για 3 έτη (άρθρο 13 παρ. 1 του ν."
          + " 4242/2014). Ισχύουν τα άρθρα 608 και 609 ΑΚ.</p><p>Βλ. άρθρο 612Α ΑΚ και π.δ."
          + " 34/1995.</p></div>";

  private static String translation(String body) {
    return "<!-- legal-basis: AK x reviewed-by: none translation: machine-drafted -->\n"
        + "<div th:fragment=\"clause-a\">"
        + body
        + "</div>";
  }

  @Test
  @DisplayName("the Greek pattern pairs the article with its paragraph and keeps the law number")
  void greekCitationPairsArticleAndParagraph() {
    assertThat(LeaseDocumentFidelityTest.citations("<p>(άρθρο 13 παρ. 1 του ν. 4242/2014)</p>", GR))
        .containsExactlyInAnyOrder("0: 13 1", "0: 4242/2014");
    assertThat(
            LeaseDocumentFidelityTest.citations(
                "<p>(article 13, paragraph 1, of Law 4242/2014)</p>", GR))
        .containsExactlyInAnyOrder("0: 13 1", "0: 4242/2014");
    assertThat(LeaseDocumentFidelityTest.citations("<p>άρθρου 612Α ΑΚ, π.δ. 34/1995</p>", GR))
        .containsExactlyInAnyOrder("0: 612", "0: 34/1995");
    assertThat(LeaseDocumentFidelityTest.citations("<p>Presidential Decree 34/1995</p>", GR))
        .containsExactly("0: 34/1995");
  }

  @Test
  @DisplayName("a faithful English translation passes, a swapped paragraph or law number fails")
  void greekCitationGate() {
    String good =
        translation(
            "<p>The lease lasts for 3 years (article 13, paragraph 1, of Law 4242/2014). Articles"
                + " 608 and 609 of the Civil Code apply.</p><p>See article 612A of the Civil Code"
                + " and Presidential Decree 34/1995.</p>");
    assertThat(LeaseDocumentFidelityTest.violations(AUTH, good, GR)).isEmpty();
    assertThat(
            LeaseDocumentFidelityTest.violations(
                AUTH, good.replace("paragraph 1, of", "paragraph 2, of"), GR))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
    assertThat(
            LeaseDocumentFidelityTest.violations(AUTH, good.replace("4242/2014", "4242/2015"), GR))
        .anyMatch(v -> v.startsWith("citations differ in clause-a"));
  }

  @Test
  @DisplayName("Greek markers are found next to Greek letters, the Latin boundary would miss them")
  void greekMarkersUnicodeBoundary() {
    Pattern markers = greekMarkers();
    assertThat(markers.matcher("the landlord ο εκμισθωτής pays").find()).isTrue();
    assertThat(markers.matcher("the landlord (ο εκμισθωτής) pays").find()).isTrue();
    assertThat(markers.matcher("the leased premises").find()).isFalse();
    assertThat(
            LeaseDocumentRenderTest.markersOutsideParentheses(
                "<p>the premises (μίσθιο) and μίσθιο</p>", markers))
        .containsExactly("μίσθιο");
  }

  private static Pattern greekMarkers() {
    return LeaseDocumentRegistry.find("GR", LeaseKind.RESIDENTIAL)
        .flatMap(LeaseDocumentRegistry.Entry::foreignMarkerPattern)
        .orElseThrow();
  }
}
