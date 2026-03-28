package com.buurman.service.imports;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ImportFileStore {

  private static final long TTL_SECONDS = 30 * 60; // 30 minutes

  public record ParsedImportData(
      List<String> columns,
      List<Map<String, String>> rows,
      String originalFileName,
      String fileFormat,
      Instant storedAt) {}

  private final ConcurrentHashMap<String, ParsedImportData> store = new ConcurrentHashMap<>();

  public String store(ParsedImportData data) {
    evictExpired();
    String key = java.util.UUID.randomUUID().toString();
    store.put(key, data);
    return key;
  }

  public Optional<ParsedImportData> get(String key) {
    evictExpired();
    return Optional.ofNullable(store.get(key))
        .filter(data -> !isExpired(data));
  }

  public void remove(String key) {
    store.remove(key);
  }

  private boolean isExpired(ParsedImportData data) {
    return data.storedAt().plusSeconds(TTL_SECONDS).isBefore(Instant.now());
  }

  private void evictExpired() {
    Instant cutoff = Instant.now().minusSeconds(TTL_SECONDS);
    store.entrySet().removeIf(entry -> entry.getValue().storedAt().isBefore(cutoff));
  }
}
