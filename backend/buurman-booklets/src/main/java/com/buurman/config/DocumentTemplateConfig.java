package com.buurman.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class DocumentTemplateConfig {

  @Bean("documentTemplateEngine")
  public TemplateEngine documentTemplateEngine(
      @Qualifier("documentMessageSource") MessageSource documentMessageSource,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return templateEngine(documentMessageSource, cacheTemplates);
  }

  /**
   * Template engine for the redesigned booklets + summary cards, bound to {@code
   * bookletMessageSource} so their {@code #{summary.*}} / enum-label keys resolve.
   */
  @Bean("bookletTemplateEngine")
  public TemplateEngine bookletTemplateEngine(
      @Qualifier("bookletMessageSource") MessageSource bookletMessageSource,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return templateEngine(bookletMessageSource, cacheTemplates);
  }

  @Bean("documentMessageSource")
  public MessageSource documentMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return messageSource(
        cacheTemplates,
        "classpath:messages/document-extension",
        "classpath:messages/document-rent-change",
        "classpath:messages/document-payment-notice",
        "classpath:messages/document-deposit-statement");
  }

  @Bean("bookletMessageSource")
  public MessageSource bookletMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return messageSource(
        cacheTemplates,
        "classpath:messages/document-property-booklet",
        "classpath:messages/document-contract-booklet",
        "classpath:messages/document-contact-booklet",
        "classpath:messages/document-enum-labels",
        "classpath:messages/document-summary-card");
  }

  // Per-booklet sources + engines: the three *-booklet bundles share generic keys (cover.title,
  // value.*), so a single merged source would resolve them to whichever basename comes first
  // (property), making the contract/contact booklets render the property strings. Each booklet
  // binds to a source scoped to ITS bundle (+ the shared enum-label & summary-card chrome, which
  // don't collide) so titles/labels resolve correctly. The matching template engine renders the
  // redesigned multi-page templates.

  @Bean("propertyBookletMessageSource")
  public MessageSource propertyBookletMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return bookletScoped(cacheTemplates, "document-property-booklet");
  }

  @Bean("contractBookletMessageSource")
  public MessageSource contractBookletMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return bookletScoped(cacheTemplates, "document-contract-booklet");
  }

  @Bean("contactBookletMessageSource")
  public MessageSource contactBookletMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return bookletScoped(cacheTemplates, "document-contact-booklet");
  }

  @Bean("propertyBookletTemplateEngine")
  public TemplateEngine propertyBookletTemplateEngine(
      @Qualifier("propertyBookletMessageSource") MessageSource source,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return templateEngine(source, cacheTemplates);
  }

  @Bean("contractBookletTemplateEngine")
  public TemplateEngine contractBookletTemplateEngine(
      @Qualifier("contractBookletMessageSource") MessageSource source,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return templateEngine(source, cacheTemplates);
  }

  @Bean("contactBookletTemplateEngine")
  public TemplateEngine contactBookletTemplateEngine(
      @Qualifier("contactBookletMessageSource") MessageSource source,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return templateEngine(source, cacheTemplates);
  }

  /** One booklet bundle + the shared enum-label & summary-card chrome (non-colliding). */
  private static MessageSource bookletScoped(boolean cacheTemplates, String bookletBundle) {
    return messageSource(
        cacheTemplates,
        "classpath:messages/" + bookletBundle,
        "classpath:messages/document-enum-labels",
        "classpath:messages/document-summary-card");
  }

  /** Thymeleaf engine over {@code templates/documents/}, pinned to the given MessageSource. */
  private static TemplateEngine templateEngine(
      MessageSource messageSource, boolean cacheTemplates) {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
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

  private static MessageSource messageSource(boolean cacheTemplates, String... basenames) {
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
