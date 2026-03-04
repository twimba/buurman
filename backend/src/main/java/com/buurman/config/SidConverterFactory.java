package com.buurman.config;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

import com.buurman.domain.Sid;

/**
 * Spring MVC converter factory so {@code @PathVariable PropertyIdentifier id} and any other Sid
 * subtype work automatically. Falls back to {@link Sid#of(String)} for the base type.
 */
@Component
public class SidConverterFactory implements ConverterFactory<String, Sid> {

  private final Map<Class<?>, Converter<String, ? extends Sid>> converterCache =
      new ConcurrentHashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Sid> Converter<String, T> getConverter(Class<T> targetType) {
    return (Converter<String, T>)
        converterCache.computeIfAbsent(targetType, type -> createConverter((Class<T>) type));
  }

  private <T extends Sid> Converter<String, T> createConverter(Class<T> targetType) {
    if (targetType == Sid.class) {
      @SuppressWarnings("unchecked")
      Converter<String, T> converter = source -> (T) Sid.of(source);
      return converter;
    }

    try {
      Method ofMethod = targetType.getMethod("of", String.class);
      return source -> {
        try {
          @SuppressWarnings("unchecked")
          T result = (T) ofMethod.invoke(null, source);
          return result;
        } catch (ReflectiveOperationException e) {
          throw new IllegalArgumentException(
              "Cannot convert '" + source + "' to " + targetType.getSimpleName(), e);
        }
      };
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          targetType.getSimpleName() + " must have a static of(String) factory method", e);
    }
  }
}
