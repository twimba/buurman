package com.buurman.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;

import com.buurman.document.DocumentTemplateSupport;

/** Thymeleaf wiring for the entity booklets and one-page summary cards. */
@Configuration
public class BookletTemplateConfig {

  /**
   * Template engine for the redesigned booklets + summary cards, bound to {@code
   * bookletMessageSource} so their {@code #{summary.*}} / enum-label keys resolve.
   */
  @Bean("bookletTemplateEngine")
  public TemplateEngine bookletTemplateEngine(
      @Qualifier("bookletMessageSource") MessageSource bookletMessageSource,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return DocumentTemplateSupport.templateEngine(bookletMessageSource, cacheTemplates);
  }

  @Bean("bookletMessageSource")
  public MessageSource bookletMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return DocumentTemplateSupport.messageSource(
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
    return DocumentTemplateSupport.templateEngine(source, cacheTemplates);
  }

  @Bean("contractBookletTemplateEngine")
  public TemplateEngine contractBookletTemplateEngine(
      @Qualifier("contractBookletMessageSource") MessageSource source,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return DocumentTemplateSupport.templateEngine(source, cacheTemplates);
  }

  @Bean("contactBookletTemplateEngine")
  public TemplateEngine contactBookletTemplateEngine(
      @Qualifier("contactBookletMessageSource") MessageSource source,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return DocumentTemplateSupport.templateEngine(source, cacheTemplates);
  }

  /** One booklet bundle + the shared enum-label & summary-card chrome (non-colliding). */
  private static MessageSource bookletScoped(boolean cacheTemplates, String bookletBundle) {
    return DocumentTemplateSupport.messageSource(
        cacheTemplates,
        "classpath:messages/" + bookletBundle,
        "classpath:messages/document-enum-labels",
        "classpath:messages/document-summary-card");
  }
}
