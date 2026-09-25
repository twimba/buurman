package com.buurman.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;

import com.buurman.document.DocumentTemplateSupport;

/**
 * Thymeleaf wiring for tenant-facing letters (extension addenda, rent change notices, formal
 * notices, deposit statements). The {@code letter.*} signature/footer chrome lives in its own
 * bundle listed first; the per-letter bundles use disjoint prefixes, so one merged source is safe.
 */
@Configuration
public class LetterTemplateConfig {

  @Bean("letterMessageSource")
  public MessageSource letterMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return DocumentTemplateSupport.messageSource(
        cacheTemplates,
        "classpath:messages/document-letter-chrome",
        "classpath:messages/document-extension",
        "classpath:messages/document-rent-change",
        "classpath:messages/document-payment-notice",
        "classpath:messages/document-deposit-statement");
  }

  @Bean("letterTemplateEngine")
  public TemplateEngine letterTemplateEngine(
      @Qualifier("letterMessageSource") MessageSource letterMessageSource,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    return DocumentTemplateSupport.templateEngine(letterMessageSource, cacheTemplates);
  }
}
