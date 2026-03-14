package com.buurman.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Thin REST-based PostHog feature flag client. Calls the /decide endpoint to evaluate all flags for
 * a given identity in a single request and caches results per distinct_id. Zero SDK dependencies —
 * uses only java.net.http and Jackson.
 */
@Slf4j
public class PostHogFlagClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private final String host;
  private final String projectApiKey;
  private final HttpClient http;
  private final ObjectMapper mapper;
  private final Duration cacheTtl;

  private final ConcurrentHashMap<String, CachedDecision> cache = new ConcurrentHashMap<>();

  public PostHogFlagClient(String host, String projectApiKey, Duration cacheTtl) {
    this.host = host.replaceAll("/+$", "");
    this.projectApiKey = projectApiKey;
    this.http = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
    this.mapper = new ObjectMapper();
    this.cacheTtl = cacheTtl;
  }

  public record FlagResult(boolean enabled, @Nullable Object payload) {}

  /**
   * Evaluates all feature flags for the given identity. Results are cached per distinct_id for the
   * configured TTL.
   */
  public Map<String, FlagResult> evaluateAll(
      String distinctId, Map<String, Object> personProperties, Map<String, String> groups) {
    CachedDecision cached = cache.get(distinctId);
    if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
      return cached.flags();
    }

    try {
      Map<String, FlagResult> flags = callDecide(distinctId, personProperties, groups);
      cache.put(distinctId, new CachedDecision(flags, Instant.now().plus(cacheTtl)));
      return flags;
    } catch (Exception e) {
      log.warn("PostHog /decide call failed for '{}': {}", distinctId, e.getMessage());
      if (cached != null) {
        return cached.flags();
      }
      return Collections.emptyMap();
    }
  }

  /** Evaluates a single flag (delegates to evaluateAll, which is cached). */
  public boolean isEnabled(
      String flagKey,
      String distinctId,
      Map<String, Object> personProperties,
      Map<String, String> groups) {
    Map<String, FlagResult> flags = evaluateAll(distinctId, personProperties, groups);
    FlagResult result = flags.get(flagKey);
    return result != null && result.enabled();
  }

  /** Gets the payload value for a flag. */
  public @Nullable Object getPayload(
      String flagKey,
      String distinctId,
      Map<String, Object> personProperties,
      Map<String, String> groups) {
    Map<String, FlagResult> flags = evaluateAll(distinctId, personProperties, groups);
    FlagResult result = flags.get(flagKey);
    return result != null ? result.payload() : null;
  }

  /** Clears the evaluation cache (called after admin mutations change flag state). */
  public void clearCache() {
    cache.clear();
  }

  private Map<String, FlagResult> callDecide(
      String distinctId, Map<String, Object> personProperties, Map<String, String> groups)
      throws Exception {
    ObjectNode body = mapper.createObjectNode();
    body.put("api_key", projectApiKey);
    body.put("distinct_id", distinctId);
    if (!personProperties.isEmpty()) {
      body.set("person_properties", mapper.valueToTree(personProperties));
    }
    if (!groups.isEmpty()) {
      body.set("groups", mapper.valueToTree(groups));
    }

    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(host + "/decide/?v=3"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .timeout(REQUEST_TIMEOUT)
            .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new RuntimeException(
          "PostHog /decide returned %d: %s".formatted(resp.statusCode(), resp.body()));
    }

    return parseDecideResponse(mapper.readTree(resp.body()));
  }

  private Map<String, FlagResult> parseDecideResponse(JsonNode resp) {
    Map<String, FlagResult> result = new HashMap<>();

    JsonNode featureFlags = resp.get("featureFlags");
    JsonNode payloads = resp.get("featureFlagPayloads");

    if (featureFlags != null && featureFlags.isObject()) {
      featureFlags
          .fields()
          .forEachRemaining(
              entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                boolean enabled;
                if (value.isBoolean()) {
                  enabled = value.asBoolean();
                } else if (value.isTextual()) {
                  enabled = !value.asText().isEmpty();
                } else {
                  enabled = false;
                }

                Object payload = null;
                if (payloads != null && payloads.has(key) && !payloads.get(key).isNull()) {
                  JsonNode p = payloads.get(key);
                  payload = p.isTextual() ? p.asText() : p.toString();
                }

                result.put(key, new FlagResult(enabled, payload));
              });
    }

    return Collections.unmodifiableMap(result);
  }

  private record CachedDecision(Map<String, FlagResult> flags, Instant expiresAt) {}
}
