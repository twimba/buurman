package com.buurman.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.buurman.domain.Ulid;

/**
 * Spring MVC converter so {@code @PathVariable Ulid id} and {@code @RequestParam Ulid id} work
 * automatically.
 */
@Component
public class UlidConverter implements Converter<String, Ulid> {

  @Override
  public Ulid convert(String source) {
    return Ulid.of(source);
  }
}
