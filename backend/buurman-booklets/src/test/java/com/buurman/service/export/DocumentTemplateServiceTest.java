package com.buurman.service.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.buurman.exception.BadRequestException;

@DisplayName("DocumentTemplateService")
class DocumentTemplateServiceTest {

  private DocumentTemplateService service;

  @BeforeEach
  void setUp() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);
    resolver.setOrder(1);
    resolver.setCheckExistence(true);

    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();
    messageSource.setBasenames("classpath:messages/test-doc");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setUseCodeAsDefaultMessage(true);

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(messageSource);

    PdfRenderer pdfRenderer = new PdfRenderer();
    service = new DocumentTemplateService(engine, pdfRenderer);
  }

  @Nested
  @DisplayName("resolveLocale")
  class ResolveLocale {

    @ParameterizedTest(name = "resolves ''{0}'' to a valid locale")
    @ValueSource(strings = {"en", "nl", "de", "fr", "pt", "es", "sv", "it"})
    void resolvesAllSupportedLanguages(String lang) {
      Locale locale = DocumentTemplateService.resolveLocale(lang);
      assertThat(locale.getLanguage()).isEqualTo(lang);
    }

    @ParameterizedTest(name = "rejects unsupported language ''{0}''")
    @ValueSource(strings = {"xx", "ja", "zh", "", "en-US", "../../etc"})
    void rejectsUnsupportedLanguages(String lang) {
      assertThatThrownBy(() -> DocumentTemplateService.resolveLocale(lang))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Unsupported document language");
    }
  }

  @Nested
  @DisplayName("renderToHtml")
  class RenderToHtml {

    @Test
    @DisplayName("renders template with English messages")
    void rendersEnglish() {
      String html =
          service.renderToHtml(
              "test-doc", Locale.ENGLISH, Map.of("name", "Alice", "showExtra", true));
      assertThat(html).contains("Test Document");
      assertThat(html).contains("Hello Alice!");
      assertThat(html).contains("Extra Content");
    }

    @Test
    @DisplayName("renders template with Dutch messages")
    void rendersDutch() {
      Locale nl = Locale.forLanguageTag("nl");
      String html =
          service.renderToHtml("test-doc", nl, Map.of("name", "Pieter", "showExtra", true));
      assertThat(html).contains("Testdocument");
      assertThat(html).contains("Hallo Pieter!");
      assertThat(html).contains("Extra Inhoud");
    }

    @Test
    @DisplayName("conditional section hidden when variable is false")
    void conditionalHidden() {
      String html =
          service.renderToHtml(
              "test-doc", Locale.ENGLISH, Map.of("name", "Bob", "showExtra", false));
      assertThat(html).contains("Hello Bob!");
      assertThat(html).doesNotContain("Extra Content");
    }

    @Test
    @DisplayName("falls back to English when locale has no specific bundle")
    void fallbackToDefault() {
      // French is supported but has no test-doc bundle — falls back to code as default
      Locale fr = Locale.forLanguageTag("fr");
      String html =
          service.renderToHtml("test-doc", fr, Map.of("name", "Pierre", "showExtra", false));
      // With useCodeAsDefaultMessage=true, missing bundles use the message key itself
      assertThat(html).contains("test.title");
    }

    @Test
    @DisplayName("throws on non-existent template")
    void throwsOnMissingTemplate() {
      assertThatThrownBy(() -> service.renderToHtml("non-existent", Locale.ENGLISH, Map.of()))
          .isInstanceOf(TemplateInputException.class);
    }
  }

  @Nested
  @DisplayName("renderToPdf")
  class RenderToPdf {

    @Test
    @DisplayName("produces non-empty PDF bytes with valid magic header")
    void producesValidPdf() {
      byte[] pdf =
          service.renderToPdf(
              "test-doc", Locale.ENGLISH, Map.of("name", "Test", "showExtra", false));
      assertThat(pdf).isNotEmpty();
      assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("PDF content differs by locale")
    void pdfDiffersByLocale() {
      byte[] enPdf =
          service.renderToPdf(
              "test-doc", Locale.ENGLISH, Map.of("name", "Test", "showExtra", false));
      Locale nl = Locale.forLanguageTag("nl");
      byte[] nlPdf =
          service.renderToPdf("test-doc", nl, Map.of("name", "Test", "showExtra", false));
      assertThat(enPdf).isNotEqualTo(nlPdf);
    }

    @ParameterizedTest(name = "renders PDF for locale ''{0}'' with valid header")
    @MethodSource("com.buurman.service.export.DocumentTemplateServiceTest#supportedLocalesForPdf")
    void rendersValidPdfForAllSupportedLocales(Locale locale) {
      byte[] pdf =
          service.renderToPdf("test-doc", locale, Map.of("name", "Test", "showExtra", false));
      assertThat(pdf).isNotEmpty();
      assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
  }

  static Stream<Locale> supportedLocalesForPdf() {
    return Stream.of("en", "nl", "de", "fr", "pt", "es", "sv", "it").map(Locale::forLanguageTag);
  }
}
