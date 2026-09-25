package com.buurman.service.export;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.buurman.document.DocumentFormatting;

/**
 * Resolves locale-aware display labels for domain enums from the {@code document-enum-labels}
 * message bundle, replacing the hardcoded English maps and {@link
 * DocumentFormatting#formatEnumValue} calls scattered across the exporters.
 *
 * <p>Key convention: {@code <lowerCamelEnumSimpleName>.<ENUM_CONSTANT>} — e.g. {@code
 * PropertyStatus.VACANT} → {@code propertyStatus.VACANT}. When a key is missing the label degrades
 * to a Title-Cased rendering of the constant name (never blank, never a raw {@code SNAKE_CASE}
 * code), matching the legacy behaviour so untranslated values stay readable.
 */
@Component
public class EnumLabelResolver {

  private final MessageSource messageSource;

  EnumLabelResolver(@Qualifier("bookletMessageSource") MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /** Resolves the label for an enum value, deriving the namespace from its simple class name. */
  public String label(@Nullable Enum<?> value, Locale locale) {
    if (value == null) {
      return "";
    }
    return label(namespace(value.getDeclaringClass()), value.name(), locale);
  }

  /**
   * Resolves a label by explicit namespace + code (for cases where the value is a raw string, e.g.
   * a JOOQ column) — e.g. {@code label("paymentMethod", "BANK_TRANSFER", locale)}.
   */
  public String label(String namespace, @Nullable String code, Locale locale) {
    if (code == null || code.isBlank()) {
      return "";
    }
    String key = namespace + "." + code;
    String fallback = DocumentFormatting.formatEnumValue(code);
    return Objects.requireNonNullElse(
        messageSource.getMessage(key, null, fallback, locale), fallback);
  }

  private static String namespace(Class<?> enumClass) {
    String name = enumClass.getSimpleName();
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }
}
