package com.buurman.config;

import com.buurman.domain.Ulid;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring MVC converter so {@code @PathVariable Ulid id} and
 * {@code @RequestParam Ulid id} work automatically.
 */
@Component
public class UlidConverter implements Converter<String, Ulid> {

    @Override
    public Ulid convert(String source) {
        return Ulid.of(source);
    }
}
