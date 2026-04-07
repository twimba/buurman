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
        "classpath:messages/document-contact-booklet");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setUseCodeAsDefaultMessage(true);
    if (!cacheTemplates) {
      source.setCacheSeconds(0);
    }
    return source;
  }
}
