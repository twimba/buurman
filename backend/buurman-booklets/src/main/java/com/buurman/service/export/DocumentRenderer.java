package com.buurman.service.export;

/**
 * Renders an HTML document to PDF bytes. The swap-point seam for the rendering engine — currently a
 * headless-Chromium/Gotenberg sidecar ({@link GotenbergDocumentRenderer}). Callers depend on this
 * interface, never on a concrete engine.
 */
public interface DocumentRenderer {

  /**
   * Renders the given (fully self-contained) HTML to a PDF using the supplied page geometry.
   *
   * @param html complete HTML document, including any inline CSS and embedded/linked assets
   * @param page target page size and orientation
   * @return PDF bytes
   */
  byte[] render(String html, PageSpec page);

  /** Convenience for the common A4-portrait case. */
  default byte[] render(String html) {
    return render(html, PageSpec.A4_PORTRAIT);
  }
}
