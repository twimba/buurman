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
public class EmailTemplateConfig {

  @Bean("emailTemplateEngine")
  public TemplateEngine emailTemplateEngine(
      @Qualifier("notificationMessageSource") MessageSource notificationMessageSource,
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/email/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(cacheTemplates);
    resolver.setOrder(1);
    resolver.setCheckExistence(true);

    // Anonymous subclass prevents Spring's MessageSourceAware callback from
    // replacing our email-specific MessageSource with the application default.
    SpringTemplateEngine engine =
        new SpringTemplateEngine() {
          @Override
          public void setMessageSource(MessageSource messageSource) {
            super.setMessageSource(notificationMessageSource);
          }
        };
    engine.setTemplateResolver(resolver);
    engine.setMessageSource(notificationMessageSource);
    return engine;
  }

  @Bean("notificationMessageSource")
  public MessageSource notificationMessageSource(
      @Value("${spring.thymeleaf.cache:true}") boolean cacheTemplates) {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasenames(
        "classpath:messages/email-subjects",
        "classpath:messages/email-bodies",
        "classpath:messages/sms-bodies");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setUseCodeAsDefaultMessage(true);
    if (!cacheTemplates) {
      source.setCacheSeconds(0);
    }
    return source;
  }
}
