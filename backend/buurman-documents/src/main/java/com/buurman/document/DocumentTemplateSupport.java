package com.buurman.document;

import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Factories for the Thymeleaf engines and message sources behind generated documents. Each document
 * family (booklets, letters) declares its own beans from these so its {@code #{...}} keys resolve
 * against exactly its bundles.
 */
public final class DocumentTemplateSupport {

  /** Classpath root of every document template: {@code templates/documents/{type}/generic.html}. */
  public static final String TEMPLATE_PREFIX = "templates/documents/";

  private DocumentTemplateSupport() {}

  /** Thymeleaf engine over {@link #TEMPLATE_PREFIX}, pinned to the given MessageSource. */
  public static TemplateEngine templateEngine(MessageSource messageSource, boolean cacheTemplates) {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix(TEMPLATE_PREFIX);
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(cacheTemplates);
    resolver.setOrder(1);
    resolver.setCheckExistence(true);

    // Anonymous subclass prevents Spring's MessageSourceAware callback from replacing our
    // document-specific MessageSource with the application default.
    SpringTemplateEngine engine =
        new SpringTemplateEngine() {
          @Override
          public void setMessageSource(MessageSource ignored) {
            super.setMessageSource(messageSource);
          }
        };
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(messageSource);
    return engine;
  }

  /** UTF-8 bundle source over the given {@code classpath:messages/...} basenames. */
  public static MessageSource messageSource(boolean cacheTemplates, String... basenames) {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasenames(basenames);
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setUseCodeAsDefaultMessage(true);
    if (!cacheTemplates) {
      source.setCacheSeconds(0);
    }
    return source;
  }
}
