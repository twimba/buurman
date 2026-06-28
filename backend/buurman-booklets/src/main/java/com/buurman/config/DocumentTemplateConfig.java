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
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(cacheTemplates);
    resolver.setOrder(1);
    resolver.setCheckExistence(true);

    // Anonymous subclass prevents Spring's MessageSourceAware callback from
    // replacing our document-specific MessageSource with the application default.
    SpringTemplateEngine engine =
        new SpringTemplateEngine() {
          @Override
          public void setMessageSource(MessageSource messageSource) {
            super.setMessageSource(documentMessageSource);
          }
        };
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(documentMessageSource);
    return engine;
  }

  /**
   * Template engine for the redesigned booklets + summary cards, bound to {@code
   * bookletMessageSource} so their {@code #{summary.*}} / enum-label keys resolve.
   */
  @Bean("bookletTemplateEngine")
  public TemplateEngine bookletTemplateEngine(
      @Qualifier("bookletMessageSource") MessageSource bookletMessageSource,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/documents/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(cacheTemplates);
    resolver.setOrder(1);
    resolver.setCheckExistence(true);

    SpringTemplateEngine engine =
        new SpringTemplateEngine() {
          @Override
          public void setMessageSource(MessageSource messageSource) {
            super.setMessageSource(bookletMessageSource);
          }
        };
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(bookletMessageSource);
    return engine;
  }

  @Bean("documentMessageSource")
  public MessageSource documentMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasenames(
        "classpath:messages/document-extension", "classpath:messages/document-rent-change");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setUseCodeAsDefaultMessage(true);
    if (!cacheTemplates) {
      source.setCacheSeconds(0);
    }
    return source;
  }

  @Bean("bookletMessageSource")
  public MessageSource bookletMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasenames(
        "classpath:messages/document-property-booklet",
        "classpath:messages/document-contract-booklet",
        "classpath:messages/document-contact-booklet",
        "classpath:messages/document-enum-labels",
        "classpath:messages/document-summary-card");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setUseCodeAsDefaultMessage(true);
    if (!cacheTemplates) {
      source.setCacheSeconds(0);
    }
    return source;
  }

  // Per-booklet sources: the three *-booklet bundles share generic keys (cover.title, value.*),
  // so a single merged source would resolve them to whichever basename comes first (property),
  // making the contract/contact booklets render the property strings. Each legacy exporter binds
  // to its own bundle to keep titles/labels correct.

  @Bean("propertyBookletMessageSource")
  public MessageSource propertyBookletMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return scopedBookletSource(cacheTemplates, "classpath:messages/document-property-booklet");
  }

  @Bean("contractBookletMessageSource")
  public MessageSource contractBookletMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return scopedBookletSource(cacheTemplates, "classpath:messages/document-contract-booklet");
  }

  @Bean("contactBookletMessageSource")
  public MessageSource contactBookletMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return scopedBookletSource(cacheTemplates, "classpath:messages/document-contact-booklet");
  }

  private static MessageSource scopedBookletSource(boolean cacheTemplates, String... basenames) {
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
