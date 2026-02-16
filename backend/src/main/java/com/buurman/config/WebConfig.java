package com.buurman.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  // CORS is configured in SecurityConfig to avoid duplicate/conflicting configuration

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverterFactory(new CaseInsensitiveEnumConverterFactory());
  }

  private static class CaseInsensitiveEnumConverterFactory
      implements ConverterFactory<String, Enum<?>> {

    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
      return source -> {
        T[] constants = targetType.getEnumConstants();
        for (T constant : constants) {
          if (constant.name().equalsIgnoreCase(source.trim())) {
            return constant;
          }
        }
        throw new IllegalArgumentException(
            "No enum constant " + targetType.getCanonicalName() + "." + source);
      };
    }
  }
}
