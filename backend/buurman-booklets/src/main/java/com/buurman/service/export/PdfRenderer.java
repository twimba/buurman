package com.buurman.service.export;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Component;

import com.buurman.exception.ExternalServiceException;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

@Component
class PdfRenderer {

  public static final PageSize PAGE_SIZE = PageSize.A4;

  byte[] renderHtml(String html) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer)) {
      pdf.setDefaultPageSize(PAGE_SIZE);

      ConverterProperties converterProperties = new ConverterProperties();
      HtmlConverter.convertToPdf(html, pdf, converterProperties);

      return baos.toByteArray();
    } catch (Exception e) {
      throw new ExternalServiceException("Failed to generate PDF", e);
    }
  }
}
