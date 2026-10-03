package com.buurman.service.letters;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.buurman.util.DocumentLanguages;

/**
 * Tells whether a lease message key is defined in a language's <em>own</em> bundle file. A {@code
 * MessageSource} cannot: a key missing from {@code _nl} silently resolves to the English base, so
 * the translation gap is invisible. Bundles are static classpath resources, so each file is parsed
 * once and cached.
 */
@Component
public class LeaseMessageCatalog {

  static final String LEASE_BUNDLE = "messages/document-lease-agreement";

  private final String basename;
  private final List<String> languages;
  private final Map<String, Properties> bundles = new ConcurrentHashMap<>();

  @Autowired
  public LeaseMessageCatalog() {
    this(LEASE_BUNDLE, DocumentLanguages.ORDERED);
  }

  LeaseMessageCatalog(String basename, List<String> languages) {
    this.basename = basename;
    this.languages = List.copyOf(languages);
  }

  /** True when the language's own file (the base file for English) has a non-blank value. */
  public boolean definedIn(String language, String key) {
    if (!languages.contains(language)) {
      return false;
    }
    String value = bundle(language).getProperty(key);
    return value != null && !value.isBlank();
  }

  /** Languages, in {@link DocumentLanguages#ORDERED} order, whose own bundle lacks the key. */
  public List<String> missingLanguages(String key) {
    return languages.stream().filter(language -> !definedIn(language, key)).toList();
  }

  private Properties bundle(String language) {
    return bundles.computeIfAbsent(language, this::load);
  }

  private Properties load(String language) {
    String suffix = DocumentLanguages.DEFAULT.equals(language) ? "" : "_" + language;
    ClassPathResource resource = new ClassPathResource(basename + suffix + ".properties");
    Properties properties = new Properties();
    if (!resource.exists()) {
      return properties;
    }
    try (InputStream in = resource.getInputStream();
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read bundle " + resource.getPath(), e);
    }
    return properties;
  }
}
