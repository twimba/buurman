package com.buurman.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class I18nConfig implements WebMvcConfigurer {

  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasenames("classpath:messages/api-errors");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setUseCodeAsDefaultMessage(true);
    return source;
  }

  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(Locale.ENGLISH);
    resolver.setSupportedLocales(
        List.of(
            Locale.ENGLISH,
            Locale.forLanguageTag("nl"),
            Locale.forLanguageTag("pt"),
            Locale.forLanguageTag("es"),
            Locale.FRENCH,
            Locale.GERMAN));
    return resolver;
  }
}
