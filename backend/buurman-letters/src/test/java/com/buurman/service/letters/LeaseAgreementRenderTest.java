package com.buurman.service.letters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentTemplateSupport;

/** Real Thymeleaf engine, real bundles, no PDF: proves the shell + clause-fragment mechanism. */
@DisplayName("lease-agreement shell render")
class LeaseAgreementRenderTest {

  private TemplateEngine engine;

  @BeforeEach
  void setUp() {
    engine =
        DocumentTemplateSupport.templateEngine(
            DocumentTemplateSupport.messageSource(
                false,
                "classpath:messages/document-letter-chrome",
                "classpath:messages/document-lease-agreement"),
            false);
  }

  private static Map<String, Object> clause(String key, String title, int article) {
    Map<String, Object> m = new HashMap<>();
    m.put("clauseKey", key);
    m.put("title", title);
    m.put("articleNumber", article);
    return m;
  }

  private String render(
      List<Map<String, Object>> clauses,
      Map<String, Integer> refs,
      boolean authoritative,
      boolean fallbackUsed) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("clauseSource", "lease-agreement/ZZ/residential/en");
    vars.put("clauses", clauses);
    vars.put("refs", refs);
    vars.put("authoritative", authoritative);
    vars.put("fallbackUsed", fallbackUsed);
    vars.put("languageUsed", "en");
    vars.put("requestedLang", "en");
    vars.put("generatedDate", "1 January 2026");
    vars.put("contractIdentifier", "CON1");
    vars.put("propertyAddress", "Street 1");
    vars.put("landlordName", "Landlord BV");
    vars.put("signatureBlocks", List.of());
    vars.put("rentComponents", List.of());
    Context ctx = new Context(Locale.ENGLISH);
    ctx.setVariables(vars);
    return engine.process("lease-agreement/_shell", ctx);
  }

  private static void assertNoLeaks(String html) {
    assertThat(html).doesNotContain("null").doesNotContain("${").doesNotContain("#{");
    assertThat(html).doesNotContain("[[");
    assertThat(html).doesNotContain("??");
  }

  @Test
  @DisplayName("renders clauses in the given (shuffled) order with article numbers and fragments")
  void shuffledOrder() {
    String html =
        render(
            List.of(clause("b", "Second by key", 1), clause("a", "First by key", 2)),
            Map.of("b", 1, "a", 2),
            true,
            false);

    assertThat(html.indexOf("Second by key")).isPositive().isLessThan(html.indexOf("First by key"));
    assertThat(html).containsPattern("<span>1</span>\\. <span>Second by key</span>");
    assertThat(html).containsPattern("<span>2</span>\\. <span>First by key</span>");
    assertThat(html.indexOf("BODY-BETA")).isPositive().isLessThan(html.indexOf("BODY-ALPHA"));
    assertThat(html).contains("BODY-ALPHA for ").contains("Landlord BV");
    assertNoLeaks(html);
  }

  @Test
  @DisplayName("cross-reference shows the referenced clause's article number")
  void crossReference() {
    String html =
        render(
            List.of(clause("a", "A", 1), clause("b", "B", 2)), Map.of("a", 1, "b", 2), true, false);

    assertThat(html).contains("see article ");
    assertThat(html).containsPattern("see article <span>[^<]*1[^<]*</span>");
    assertNoLeaks(html);
  }

  @Test
  @DisplayName("a reference to an excluded clause is omitted and leaves no template residue")
  void excludedReferenceOmitted() {
    String html = render(List.of(clause("b", "B", 1)), Map.of("b", 1), true, false);

    assertThat(html).contains("BODY-BETA").doesNotContain("see article");
    assertNoLeaks(html);
  }

  @Test
  @DisplayName("non-authoritative document carries the courtesy notice, authoritative does not")
  void courtesyNotice() {
    List<Map<String, Object>> clauses = new ArrayList<>(List.of(clause("a", "A", 1)));
    String courtesy = render(clauses, Map.of("a", 1), false, false);
    String authoritative = render(clauses, Map.of("a", 1), true, false);

    assertThat(courtesy).contains("courtesy translation");
    assertThat(authoritative).doesNotContain("courtesy translation");
  }

  @Test
  @DisplayName("fallback notice appears only when the requested language was not available")
  void fallbackNotice() {
    List<Map<String, Object>> clauses = List.of(clause("a", "A", 1));
    assertThat(render(clauses, Map.of("a", 1), true, true))
        .contains("not available")
        .contains("(en)");
    assertThat(render(clauses, Map.of("a", 1), true, false)).doesNotContain("not available");
  }

  @Test
  @DisplayName("the draft disclaimer is always present")
  void disclaimerAlwaysPresent() {
    List<Map<String, Object>> clauses = List.of(clause("a", "A", 1));
    assertThat(render(clauses, Map.of("a", 1), true, false)).contains("qualified legal counsel");
    assertThat(render(clauses, Map.of("a", 1), false, true)).contains("qualified legal counsel");
  }
}
