package com.buurman.dto.response.backoffice;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.LeaseAvailability;
import com.buurman.domain.LeaseKind;

/**
 * Result of a lease agreement preview. When the country has no lease ({@code availability} is
 * {@code UNAVAILABLE_*}) {@code html}, {@code languageUsed}, {@code kindUsed} and {@code source}
 * are null and {@code clauses} is empty; that is a valid answer, not an error.
 *
 * @param html self-contained HTML (inline CSS, CSP meta, SAMPLE banner), safe for a sandboxed
 *     iframe
 * @param languageUsed the language the document was rendered in (may differ from the requested one)
 * @param kindUsed the lease kind whose clause templates applied (first of the fallback chain)
 * @param clauses every resolved clause, in document order: what the resolver decided
 */
public record LeaseAgreementPreviewResponse(
    LeaseAvailability availability,
    @Nullable String html,
    @Nullable String languageUsed,
    @Nullable LeaseKind kindUsed,
    @Nullable Source source,
    List<Clause> clauses) {

  /** Whether the HTML is a reviewed per-language document or the placeholder example text. */
  public enum Source {
    DOCUMENT,
    EXAMPLE_TEXT
  }

  public record Clause(
      String clauseKey,
      String title,
      boolean included,
      boolean optional,
      boolean pinned,
      int articleNumber,
      int sortOrder) {}
}
