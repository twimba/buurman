package com.buurman.service.export;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.exception.BadRequestException;

/**
 * Resolves and renders Thymeleaf document templates with i18n support via MessageSource bundles.
 *
 * <p>Template location: {@code templates/documents/{documentType}/generic.html}
 *
 * <p>Country-specific sections are handled within templates via {@code th:if} conditionals on
 * {@code countryCode}.
 */
@Service
public class DocumentTemplateService {

  private final TemplateEngine templateEngine;
  private final DocumentRenderer pdfRenderer;

  public DocumentTemplateService(
      @Qualifier("documentTemplateEngine") TemplateEngine templateEngine,
      DocumentRenderer pdfRenderer) {
    this.templateEngine = templateEngine;
    this.pdfRenderer = pdfRenderer;
  }

  /**
   * Renders a document template to PDF bytes.
   *
   * @param documentType the document type directory (e.g., "extension-addendum")
   * @param locale the locale for i18n string resolution and date/currency formatting
   * @param variables template variables (must include "countryCode" for country-specific sections)
   * @return PDF bytes
   */
  public byte[] renderToPdf(String documentType, Locale locale, Map<String, Object> variables) {
    return renderToPdf(documentType, locale, variables, PageSpec.A4_PORTRAIT);
  }

  /**
   * Renders a document template to PDF bytes with an explicit page geometry (e.g. A4 landscape for
   * one-page summary cards).
   */
  public byte[] renderToPdf(
      String documentType, Locale locale, Map<String, Object> variables, PageSpec page) {
    String html = renderToHtml(documentType, locale, variables);
    return pdfRenderer.render(html, page);
  }

  /**
   * Renders a document template to HTML string.
   *
   * @param documentType the document type directory (e.g., "extension-addendum")
   * @param locale the locale for i18n string resolution and date/currency formatting
   * @param variables template variables
   * @return rendered HTML
   */
  public String renderToHtml(String documentType, Locale locale, Map<String, Object> variables) {
    Context context = new Context(locale);
    context.setVariables(variables);
    return templateEngine.process(documentType + "/generic", context);
  }

  private static final Set<String> SUPPORTED_LANGUAGES =
      Set.of("en", "nl", "de", "fr", "pt", "es", "sv", "it", "fi", "el", "pl", "da", "nb");

  /**
   * Resolves a Locale from an ISO 639-1 language code.
   *
   * @param lang language code (e.g., "en", "nl", "de")
   * @return the corresponding Locale
   * @throws BadRequestException if the language is not supported
   */
  public static Locale resolveLocale(String lang) {
    if (!SUPPORTED_LANGUAGES.contains(lang)) {
      throw new BadRequestException("Unsupported document language: " + lang);
    }
    return Locale.forLanguageTag(lang);
  }
}
