package com.buurman.config;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

import com.buurman.domain.Ulid;

/**
 * Spring MVC converter factory so {@code @PathVariable PropertyIdentifier id} and any other Ulid
 * subtype work automatically. Falls back to {@link Ulid#of(String)} for the base type.
 */
@Component
public class UlidConverterFactory implements ConverterFactory<String, Ulid> {

  private final Map<Class<?>, Converter<String, ? extends Ulid>> converterCache =
      new ConcurrentHashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Ulid> Converter<String, T> getConverter(Class<T> targetType) {
    return (Converter<String, T>)
        converterCache.computeIfAbsent(targetType, type -> createConverter((Class<T>) type));
  }

  private <T extends Ulid> Converter<String, T> createConverter(Class<T> targetType) {
    if (targetType == Ulid.class) {
      @SuppressWarnings("unchecked")
      Converter<String, T> converter = source -> (T) Ulid.of(source);
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
