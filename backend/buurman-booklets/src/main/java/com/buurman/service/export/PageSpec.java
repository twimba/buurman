package com.buurman.service.export;

/**
 * Renderer-agnostic description of the target page geometry for a generated document. Kept free of
 * any rendering-engine types so it can drive iText today and a headless-Chromium (Gotenberg)
 * renderer tomorrow.
 */
public record PageSpec(PaperSize size, Orientation orientation) {

  public enum PaperSize {
    A4
  }

  public enum Orientation {
    PORTRAIT,
    LANDSCAPE
  }

  /** Standard A4 portrait — the default for multi-page booklets and legal documents. */
  public static final PageSpec A4_PORTRAIT = new PageSpec(PaperSize.A4, Orientation.PORTRAIT);

  /** A4 landscape — used by the one-page entity summary cards. */
  public static final PageSpec A4_LANDSCAPE = new PageSpec(PaperSize.A4, Orientation.LANDSCAPE);

  public boolean isLandscape() {
    return orientation == Orientation.LANDSCAPE;
  }
}
