package com.buurman.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

  public GeocodingService(GoogleMapsProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
  }

  public record GeocodingResult(BigDecimal latitude, BigDecimal longitude, String accuracy) {}

  /** Geocode with progressive fallback: full address → city+country → country only. */
  public Optional<GeocodingResult> geocode(
      String street, String city, String postalCode, String country) {
    if (properties.apiKey() == null || properties.apiKey().isBlank()) {
      log.debug("Google Maps API key not configured, skipping geocoding");
      return Optional.empty();
    }

    // Try 1: Full address
    String fullAddress =
        Stream.of(street, city, postalCode, country)
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining(", "));
    Optional<GeocodingResult> result = geocodeAddress(fullAddress);
    if (result.isPresent()) {
      return result;
    }

    // Try 2: City + country
    String cityCountry =
        Stream.of(city, country)
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining(", "));
    if (!cityCountry.equals(fullAddress)) {
      result = geocodeAddress(cityCountry);
      if (result.isPresent()) {
        log.debug("Geocoding fell back to city+country for: {}", fullAddress);
        return Optional.of(
            new GeocodingResult(result.get().latitude(), result.get().longitude(), "CITY"));
      }
    }

    // Try 3: Country only
    if (country != null && !country.isBlank()) {
      result = geocodeAddress(country);
      if (result.isPresent()) {
        log.debug("Geocoding fell back to country for: {}", fullAddress);
        return Optional.of(
            new GeocodingResult(result.get().latitude(), result.get().longitude(), "COUNTRY"));
      }
    }

    log.debug("Geocoding returned no results at any level for: {}", fullAddress);
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
      return Optional.empty();
    }
  }
}
