package com.buurman.service.letters;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.buurman.document.DocumentLocale;
import com.buurman.document.DocumentRenderer;
import com.buurman.document.PageSpec;
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
public class LetterTemplateService {

  private final TemplateEngine templateEngine;
  private final DocumentRenderer pdfRenderer;

  public LetterTemplateService(
      @Qualifier("letterTemplateEngine") TemplateEngine templateEngine,
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
    String html = renderToHtml(documentType, locale, variables);
    return pdfRenderer.render(html, PageSpec.A4_PORTRAIT);
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
    return renderTemplateToHtml(documentType + "/generic", locale, variables);
  }

  /**
   * Renders an explicit template (name relative to {@code templates/documents/}, no extension) to
   * PDF bytes, instead of the {@code documentType/generic} convention.
   */
  public byte[] renderToPdfTemplate(
      String templateName, Locale locale, Map<String, Object> variables) {
    String html = renderTemplateToHtml(templateName, locale, variables);
    return pdfRenderer.render(html, PageSpec.A4_PORTRAIT);
  }

  /** Renders an explicit template name to an HTML string. */
  public String renderTemplateToHtml(
      String templateName, Locale locale, Map<String, Object> variables) {
    Context context = new Context(locale);
    context.setVariables(variables);
    return templateEngine.process(templateName, context);
  }

  /**
   * Resolves a Locale from an ISO 639-1 language code.
   *
   * @throws BadRequestException if the language is not supported
   */
  public static Locale resolveLocale(String lang) {
    return DocumentLocale.resolve(lang);
  }
}
