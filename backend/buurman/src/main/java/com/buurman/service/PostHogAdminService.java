package com.buurman.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.buurman.config.PostHogFlagClient;
import com.buurman.config.models.PostHogProperties;
import com.buurman.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Wraps the PostHog REST API for admin operations (flag management, cohort listing, identity/cohort
 * overrides). Uses the personal API key for authentication.
 */
@Service
@Slf4j
public class PostHogAdminService {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final PostHogProperties properties;
  private final HttpClient http;
  private final ObjectMapper mapper;
  private final String baseUrl;
  private final Optional<PostHogFlagClient> flagClient;

  private Optional<Long> projectId;

  public PostHogAdminService(
      PostHogProperties properties,
      ObjectMapper mapper,
      Optional<PostHogFlagClient> flagClient) {
    this.properties = properties;
    this.http = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
    this.mapper = mapper;
    this.baseUrl = properties.host().replaceAll("/+$", "");
    this.projectId = properties.projectId();
    this.flagClient = flagClient;
  }

  // --- Public records ---

  public record FeatureFlagInfo(
      long id, String key, boolean active, @Nullable Object payload, JsonNode filters) {}

  public record CohortInfo(long id, String name, @Nullable String description) {}

  // --- Feature flag operations ---

  public List<FeatureFlagInfo> listFeatureFlags() {
    long pid = resolveProjectId();
    JsonNode resp = get("/api/projects/" + pid + "/feature_flags/?limit=1000");
    List<FeatureFlagInfo> result = new ArrayList<>();
    for (JsonNode flag : asResults(resp)) {
      if (!flag.path("deleted").asBoolean(false)) {
        result.add(parseFeatureFlag(flag));
      }
    }
    return result;
  }

  public Optional<FeatureFlagInfo> findFeatureFlagByKey(String key) {
    return listFeatureFlags().stream().filter(f -> f.key().equals(key)).findFirst();
  }

  public FeatureFlagInfo updateFeatureFlag(
      long flagId, @Nullable Boolean active, @Nullable String value) {
    long pid = resolveProjectId();

    ObjectNode body = mapper.createObjectNode();
    if (active != null) {
      body.put("active", active);
    }

    if (value != null) {
      JsonNode current = get("/api/projects/" + pid + "/feature_flags/" + flagId + "/");
      ObjectNode filters =
          current.has("filters") && !current.get("filters").isNull()
              ? (ObjectNode) current.get("filters").deepCopy()
              : mapper.createObjectNode();

      ObjectNode payloads =
          filters.has("payloads") && !filters.get("payloads").isNull()
              ? (ObjectNode) filters.get("payloads")
              : mapper.createObjectNode();

      if (value.isEmpty()) {
        payloads.remove("true");
      } else {
        payloads.put("true", value);
      }
      filters.set("payloads", payloads);
      body.set("filters", filters);
    }

    JsonNode resp = patch("/api/projects/" + pid + "/feature_flags/" + flagId + "/", body);
    invalidateCache();
    return parseFeatureFlag(resp);
  }

  // --- Identity override operations ---

  /**
   * Adds or updates a filter group on the flag that targets a specific distinct_id. The group uses
   * rollout_percentage=100 for enabled, 0 for disabled.
   */
  public void upsertIdentityOverride(long flagId, String distinctId, boolean enabled) {
    long pid = resolveProjectId();
    JsonNode current = get("/api/projects/" + pid + "/feature_flags/" + flagId + "/");
    ObjectNode filters = getOrCreateFilters(current);
    ArrayNode groups =
        filters.has("groups") ? (ArrayNode) filters.get("groups") : mapper.createArrayNode();

    int existingIdx = findIdentityOverrideGroupIndex(groups, distinctId);
    ObjectNode group = buildIdentityOverrideGroup(distinctId, enabled);

    if (existingIdx >= 0) {
      groups.set(existingIdx, group);
    } else {
      // Insert at beginning — overrides have higher priority
      ArrayNode newGroups = mapper.createArrayNode();
      newGroups.add(group);
      for (int i = 0; i < groups.size(); i++) {
        newGroups.add(groups.get(i));
      }
      groups = newGroups;
    }

    filters.set("groups", groups);
    ObjectNode body = mapper.createObjectNode();
    body.set("filters", filters);
    patch("/api/projects/" + pid + "/feature_flags/" + flagId + "/", body);
    invalidateCache();
  }

  public void deleteIdentityOverride(long flagId, String distinctId) {
    long pid = resolveProjectId();
    JsonNode current = get("/api/projects/" + pid + "/feature_flags/" + flagId + "/");
    ObjectNode filters = getOrCreateFilters(current);
    ArrayNode groups =
        filters.has("groups") ? (ArrayNode) filters.get("groups") : mapper.createArrayNode();

    int idx = findIdentityOverrideGroupIndex(groups, distinctId);
    if (idx >= 0) {
      groups.remove(idx);
      filters.set("groups", groups);
      ObjectNode body = mapper.createObjectNode();
      body.set("filters", filters);
      patch("/api/projects/" + pid + "/feature_flags/" + flagId + "/", body);
      invalidateCache();
    }
  }

  // --- Cohort operations ---

  public List<CohortInfo> listCohorts() {
    long pid = resolveProjectId();
    JsonNode resp = get("/api/projects/" + pid + "/cohorts/?limit=1000");
    List<CohortInfo> result = new ArrayList<>();
    for (JsonNode cohort : asResults(resp)) {
      if (!cohort.path("deleted").asBoolean(false) && !cohort.path("is_static").asBoolean(false)) {
        result.add(
            new CohortInfo(
                cohort.get("id").asLong(),
                cohort.path("name").asText(""),
                text(cohort, "description")));
      }
    }
    return result;
  }

  /**
   * Inspects all feature flags and returns, for the given cohort, a map of flagKey → enabled for
   * flags that have an override group targeting that cohort.
   */
  public Map<String, Boolean> getCohortOverridesFromFlags(long cohortId) {
    Map<String, Boolean> result = new HashMap<>();
    long pid = resolveProjectId();
    JsonNode resp = get("/api/projects/" + pid + "/feature_flags/?limit=1000");
    for (JsonNode flag : asResults(resp)) {
      if (flag.path("deleted").asBoolean(false)) {
        continue;
      }
      String key = flag.get("key").asText();
      JsonNode filters = flag.get("filters");
      if (filters == null || filters.isNull()) {
        continue;
      }
      JsonNode groups = filters.get("groups");
      if (groups == null || !groups.isArray()) {
        continue;
      }

      int idx = findCohortOverrideGroupIndex((ArrayNode) groups, cohortId);
      if (idx >= 0) {
        int rollout = groups.get(idx).path("rollout_percentage").asInt(0);
        result.put(key, rollout > 0);
      }
    }
    return result;
  }

  public void upsertCohortOverride(long flagId, long cohortId, boolean enabled) {
    long pid = resolveProjectId();
    JsonNode current = get("/api/projects/" + pid + "/feature_flags/" + flagId + "/");
    ObjectNode filters = getOrCreateFilters(current);
    ArrayNode groups =
        filters.has("groups") ? (ArrayNode) filters.get("groups") : mapper.createArrayNode();

    int existingIdx = findCohortOverrideGroupIndex(groups, cohortId);
    ObjectNode group = buildCohortOverrideGroup(cohortId, enabled);

    if (existingIdx >= 0) {
      groups.set(existingIdx, group);
    } else {
      ArrayNode newGroups = mapper.createArrayNode();
      newGroups.add(group);
      for (int i = 0; i < groups.size(); i++) {
        newGroups.add(groups.get(i));
      }
      groups = newGroups;
    }

    filters.set("groups", groups);
    ObjectNode body = mapper.createObjectNode();
    body.set("filters", filters);
    patch("/api/projects/" + pid + "/feature_flags/" + flagId + "/", body);
    invalidateCache();
  }

  public void deleteCohortOverride(long flagId, long cohortId) {
    long pid = resolveProjectId();
    JsonNode current = get("/api/projects/" + pid + "/feature_flags/" + flagId + "/");
    ObjectNode filters = getOrCreateFilters(current);
    ArrayNode groups =
        filters.has("groups") ? (ArrayNode) filters.get("groups") : mapper.createArrayNode();

    int idx = findCohortOverrideGroupIndex(groups, cohortId);
    if (idx >= 0) {
      groups.remove(idx);
      filters.set("groups", groups);
      ObjectNode body = mapper.createObjectNode();
      body.set("filters", filters);
      patch("/api/projects/" + pid + "/feature_flags/" + flagId + "/", body);
      invalidateCache();
    }
  }

  // --- Admin status ---

  public boolean isAdminConfigured() {
    return properties.personalApiKey().filter(s -> !s.isBlank()).isPresent();
  }

  public String getAuthMethod() {
    return isAdminConfigured() ? "personal_api_key" : "none";
  }

  // --- Internal: parsing ---

  private FeatureFlagInfo parseFeatureFlag(JsonNode flag) {
    long id = flag.get("id").asLong();
    String key = flag.get("key").asText();
    boolean active = flag.path("active").asBoolean(false);
    Object payload = extractPayload(flag);
    JsonNode filters =
        flag.has("filters") && !flag.get("filters").isNull()
            ? flag.get("filters")
            : mapper.createObjectNode();
    return new FeatureFlagInfo(id, key, active, payload, filters);
  }

  private @Nullable Object extractPayload(JsonNode flag) {
    JsonNode filters = flag.get("filters");
    if (filters == null || filters.isNull()) {
      return null;
    }
    JsonNode payloads = filters.get("payloads");
    if (payloads == null || payloads.isNull() || !payloads.isObject()) {
      return null;
    }
    JsonNode truePayload = payloads.get("true");
    if (truePayload == null || truePayload.isNull()) {
      return null;
    }
    return truePayload.asText();
  }

  // --- Internal: filter group helpers ---

  private ObjectNode getOrCreateFilters(JsonNode flag) {
    return flag.has("filters") && !flag.get("filters").isNull()
        ? (ObjectNode) flag.get("filters").deepCopy()
        : mapper.createObjectNode();
  }

  private int findIdentityOverrideGroupIndex(ArrayNode groups, String distinctId) {
    for (int i = 0; i < groups.size(); i++) {
      JsonNode group = groups.get(i);
      JsonNode properties = group.get("properties");
      if (properties == null || !properties.isArray()) {
        continue;
      }
      for (JsonNode prop : properties) {
        if ("distinct_id".equals(text(prop, "key")) && "person".equals(text(prop, "type"))) {
          JsonNode values = prop.get("value");
          if (values != null && values.isArray()) {
            for (JsonNode v : values) {
              if (distinctId.equals(v.asText())) {
                return i;
              }
            }
          }
        }
      }
    }
    return -1;
  }

  private int findCohortOverrideGroupIndex(ArrayNode groups, long cohortId) {
    for (int i = 0; i < groups.size(); i++) {
      JsonNode group = groups.get(i);
      JsonNode properties = group.get("properties");
      if (properties == null || !properties.isArray()) {
        continue;
      }
      for (JsonNode prop : properties) {
        if ("id".equals(text(prop, "key")) && "cohort".equals(text(prop, "type"))) {
          JsonNode value = prop.get("value");
          if (value != null) {
            if (value.isArray()) {
              for (JsonNode v : value) {
                if (v.asLong() == cohortId) {
                  return i;
                }
              }
            } else if (value.asLong() == cohortId) {
              return i;
            }
          }
        }
      }
    }
    return -1;
  }

  private ObjectNode buildIdentityOverrideGroup(String distinctId, boolean enabled) {
    ObjectNode group = mapper.createObjectNode();
    ArrayNode properties = mapper.createArrayNode();
    ObjectNode prop = mapper.createObjectNode();
    prop.put("key", "distinct_id");
    prop.put("type", "person");
    ArrayNode values = mapper.createArrayNode();
    values.add(distinctId);
    prop.set("value", values);
    prop.put("operator", "exact");
    properties.add(prop);
    group.set("properties", properties);
    group.put("rollout_percentage", enabled ? 100 : 0);
    return group;
  }

  private ObjectNode buildCohortOverrideGroup(long cohortId, boolean enabled) {
    ObjectNode group = mapper.createObjectNode();
    ArrayNode properties = mapper.createArrayNode();
    ObjectNode prop = mapper.createObjectNode();
    prop.put("key", "id");
    prop.put("type", "cohort");
    prop.put("value", cohortId);
    prop.put("operator", "in");
    properties.add(prop);
    group.set("properties", properties);
    group.put("rollout_percentage", enabled ? 100 : 0);
    return group;
  }

  // --- Internal: project ID ---

  private long resolveProjectId() {
    if (projectId.isPresent()) {
      return projectId.get();
    }
    JsonNode resp = get("/api/projects/");
    JsonNode results = asResults(resp);
    if (!results.elements().hasNext()) {
      throw new ExternalServiceException("No PostHog projects found");
    }
    long id = results.iterator().next().get("id").asLong();
    this.projectId = Optional.of(id);
    log.info("PostHog admin: auto-discovered project ID {}", id);
    return id;
  }

  private void invalidateCache() {
    flagClient.ifPresent(PostHogFlagClient::clearCache);
  }

  // --- Internal: HTTP helpers ---

  private String getPersonalApiKey() {
    return properties
        .personalApiKey()
        .filter(s -> !s.isBlank())
        .orElseThrow(
            () ->
                new ExternalServiceException(
                    "PostHog personal API key not configured."
                        + " Set POSTHOG_PERSONAL_API_KEY for backoffice flag management"));
  }

  private JsonNode get(String path) {
    try {
      var req =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + path))
              .header("Authorization", "Bearer " + getPersonalApiKey())
              .GET()
              .timeout(REQUEST_TIMEOUT)
              .build();
      HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
      checkStatus(resp, "GET", path);
      return mapper.readTree(resp.body());
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new ExternalServiceException(
          "PostHog API GET %s failed: %s".formatted(path, e.getMessage()), e);
    }
  }

  private JsonNode patch(String path, JsonNode payload) {
    try {
      var req =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + path))
              .header("Authorization", "Bearer " + getPersonalApiKey())
              .header("Content-Type", "application/json")
              .method(
                  "PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
              .timeout(REQUEST_TIMEOUT)
              .build();
      HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
      checkStatus(resp, "PATCH", path);
      return mapper.readTree(resp.body());
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new ExternalServiceException(
          "PostHog API PATCH %s failed: %s".formatted(path, e.getMessage()), e);
    }
  }

  private void checkStatus(HttpResponse<String> resp, String method, String path) {
    int status = resp.statusCode();
    if (status >= 200 && status < 300) {
      return;
    }
    throw new ExternalServiceException(
        "PostHog API %s %s returned %d: %s".formatted(method, path, status, resp.body()));
  }

  private static JsonNode asResults(JsonNode node) {
    if (node.isArray()) {
      return node;
    }
    if (node.has("results")) {
      return node.get("results");
    }
    throw new IllegalStateException("Unexpected PostHog response format");
  }

  private static @Nullable String text(JsonNode node, String field) {
    return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
  }
}
