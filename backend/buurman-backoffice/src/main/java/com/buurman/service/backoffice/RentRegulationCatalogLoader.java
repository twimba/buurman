package com.buurman.service.backoffice;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.buurman.domain.regulation.RentRegulationCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Loads and caches the canonical rent-regulation dataset bundled with the application as a
 * classpath resource. The dataset is immutable at runtime, so it is parsed once and reused.
 */
@Component
@RequiredArgsConstructor
public class RentRegulationCatalogLoader {

  static final String CATALOG_RESOURCE = "rent-regulations/rent-regulations.json";

  private final ObjectMapper objectMapper;

  @Nullable private volatile RentRegulationCatalog cached;

  public RentRegulationCatalog load() {
    RentRegulationCatalog local = cached;
    if (local == null) {
      synchronized (this) {
        local = cached;
        if (local == null) {
          local = parse();
          cached = local;
        }
      }
    }
    return local;
  }

  private RentRegulationCatalog parse() {
    ClassPathResource resource = new ClassPathResource(CATALOG_RESOURCE);
    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, RentRegulationCatalog.class);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to load bundled rent-regulation catalog: " + CATALOG_RESOURCE, e);
    }
  }
}
