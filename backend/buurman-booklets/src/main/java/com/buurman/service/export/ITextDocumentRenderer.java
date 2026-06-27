package com.buurman.service.export;

import java.io.ByteArrayOutputStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.buurman.exception.DocumentRenderException;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

/**
 * iText7 {@code html2pdf} implementation of {@link DocumentRenderer}.
 *
 * <p>This is the default/fallback engine. It is active unless {@code booklet.renderer=gotenberg} is
 * configured, in which case {@link GotenbergDocumentRenderer} takes over. iText's HTML/CSS support
 * is limited (no real flexbox/grid) and the dependency is AGPL-licensed — both reasons the project
 * is migrating to a Chromium-based renderer.
 */
@Component
@ConditionalOnProperty(name = "booklet.renderer", havingValue = "itext", matchIfMissing = true)
class ITextDocumentRenderer implements DocumentRenderer {

  @Override
  public byte[] render(String html, PageSpec page) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer)) {
      pdf.setDefaultPageSize(toPageSize(page));

      ConverterProperties converterProperties = new ConverterProperties();
      HtmlConverter.convertToPdf(html, pdf, converterProperties);

      return baos.toByteArray();
    } catch (Exception e) {
      throw new DocumentRenderException("Failed to generate PDF", e);
    }
  }

  private static PageSize toPageSize(PageSpec page) {
    PageSize a4 = PageSize.A4;
    return page.isLandscape() ? a4.rotate() : a4;
  }
}
