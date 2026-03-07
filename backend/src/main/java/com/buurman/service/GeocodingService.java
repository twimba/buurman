package com.buurman.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.buurman.config.models.GoogleMapsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeocodingService {
  private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private final GoogleMapsProperties properties;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final MetricsService metricsService;

  public GeocodingService(
      GoogleMapsProperties properties, ObjectMapper objectMapper, MetricsService metricsService) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.metricsService = metricsService;
    this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
  }

  public record GeocodingResult(BigDecimal latitude, BigDecimal longitude, String accuracy) {}

  /** Geocode with progressive fallback: full address → city+country → country only. */
  public Optional<GeocodingResult> geocode(
      String street, String city, Optional<String> postalCode, String countryCode) {
    if (properties.apiKey() == null || properties.apiKey().isBlank()) {
      log.debug("Google Maps API key not configured, skipping geocoding");
      return Optional.empty();
    }

    Instant start = Instant.now();

    // Try 1: Full address
    String fullAddress =
        Stream.of(Optional.of(street), Optional.of(city), postalCode, Optional.of(countryCode))
            .flatMap(Optional::stream)
            .filter(s -> !s.isBlank())
            .collect(Collectors.joining(", "));
    Optional<GeocodingResult> result = geocodeAddress(fullAddress);
    if (result.isPresent()) {
      recordMetrics(start, "success", result.get().accuracy());
      return result;
    }

    // Try 2: City + country
    String cityCountry =
        Stream.of(city, countryCode).filter(s -> !s.isBlank()).collect(Collectors.joining(", "));
    if (!cityCountry.equals(fullAddress)) {
      result = geocodeAddress(cityCountry);
      if (result.isPresent()) {
        log.debug("Geocoding fell back to city+country for: {}", fullAddress);
        var fallbackResult =
            new GeocodingResult(result.get().latitude(), result.get().longitude(), "CITY");
        recordMetrics(start, "success", "CITY");
        return Optional.of(fallbackResult);
      }
    }

    // Try 3: Country only
    if (!countryCode.isBlank()) {
      result = geocodeAddress(countryCode);
      if (result.isPresent()) {
        log.debug("Geocoding fell back to country for: {}", fullAddress);
        var fallbackResult =
            new GeocodingResult(result.get().latitude(), result.get().longitude(), "COUNTRY");
        recordMetrics(start, "success", "COUNTRY");
        return Optional.of(fallbackResult);
      }
    }

    log.debug("Geocoding returned no results at any level for: {}", fullAddress);
    recordMetrics(start, "no_result", "NONE");
    return Optional.empty();
  }

  private Optional<GeocodingResult> geocodeAddress(String address) {
    try {
      URI uri =
          URI.create(
              GEOCODE_URL
                  + "?address="
                  + URLEncoder.encode(address, StandardCharsets.UTF_8)
                  + "&key="
                  + properties.apiKey());

      HttpRequest request =
          HttpRequest.newBuilder().uri(uri).GET().timeout(REQUEST_TIMEOUT).build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      JsonNode root = objectMapper.readTree(response.body());
      String status = root.path("status").asText();

      if ("OK".equals(status)
          && root.path("results").isArray()
          && !root.path("results").isEmpty()) {
        JsonNode firstResult = root.path("results").get(0);
        JsonNode geometry = firstResult.path("geometry");
        JsonNode location = geometry.path("location");
        BigDecimal lat = new BigDecimal(location.path("lat").asText());
        BigDecimal lng = new BigDecimal(location.path("lng").asText());

        String locationType = geometry.path("location_type").asText("APPROXIMATE");
        return Optional.of(new GeocodingResult(lat, lng, locationType));
      }

      return Optional.empty();
    } catch (Exception e) {
      log.warn("Geocoding failed for address '{}': {}", address, e.getMessage());
      metricsService.incrementCounter("geocoding.error.total");
      return Optional.empty();
    }
  }

  private void recordMetrics(Instant start, String result, String accuracy) {
    Duration duration = Duration.between(start, Instant.now());
    metricsService.recordTimer(
        "geocoding.seconds", duration, "result", result, "accuracy", accuracy);
    metricsService.incrementCounter("geocoding.total", "result", result, "accuracy", accuracy);
  }
}
