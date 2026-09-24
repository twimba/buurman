package com.buurman.document;

/**
 * Target page orientation for a generated document. Paper is always A4 (the only size the renderer
 * emits), so this carries just the orientation; the named constants read clearly at call sites
 * ({@code render(html, PageSpec.A4_LANDSCAPE)}).
 */
public record PageSpec(Orientation orientation) {

  public enum Orientation {
    PORTRAIT,
    LANDSCAPE
  }

  /** Standard A4 portrait — the default for multi-page booklets and legal documents. */
  public static final PageSpec A4_PORTRAIT = new PageSpec(Orientation.PORTRAIT);

  /** A4 landscape — used by the one-page entity summary cards. */
  public static final PageSpec A4_LANDSCAPE = new PageSpec(Orientation.LANDSCAPE);

  public boolean isLandscape() {
    return orientation == Orientation.LANDSCAPE;
  }
}
