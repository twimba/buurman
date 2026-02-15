package com.buurman.service;

import com.buurman.config.models.FlagsmithProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wraps the Flagsmith Admin REST API for write operations (flag toggles,
 * value updates, identity overrides). Read operations continue to go through
 * the Flagsmith Java SDK via {@link FeatureFlagService}.
 */
@Service
public class FlagsmithAdminService {

    private static final Logger log = LoggerFactory.getLogger(FlagsmithAdminService.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final FlagsmithProperties properties;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;

    private static final String SERVER_KEY_NAME = "Backend";

    // Cached admin session state
    private String adminToken;
    private Instant tokenExpiresAt = Instant.MIN;
    private Integer projectId;
    private Integer environmentId;
    private String environmentClientKey;
    private String serverSideKey;

    public FlagsmithAdminService(FlagsmithProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
        this.mapper = new ObjectMapper();
        this.baseUrl = resolveBaseUrl(properties.apiUrl());
    }

    // --- Public records ---

    public record FeatureStateInfo(
            long featureStateId,
            long featureId,
            String featureName,
            boolean enabled,
            Object value
    ) {}

    public record IdentityInfo(
            long id,
            String identifier
    ) {}

    public record IdentityOverrideInfo(
            long featureStateId,
            long featureId,
            String featureName,
            boolean enabled,
            Object value
    ) {}

    public record SegmentInfo(
            long id,
            String name,
            String description
    ) {}

    public record SegmentOverrideState(
            long featureStateId,
            long featureId,
            String featureName,
            boolean enabled,
            Object value
    ) {}

    public record SegmentWithOverrides(
            long segmentId,
            String segmentName,
            String description,
            List<SegmentOverrideState> overrides
    ) {}

    // --- Global feature state operations ---

    /**
     * Fetches all feature states for the environment, joining with the project's
     * feature list to resolve names (the featurestates endpoint returns feature
     * as a flat integer ID, not a nested object).
     */
    public List<FeatureStateInfo> listFeatureStates() {
        ensureDiscovered();
        Map<Long, String> featureNames = loadFeatureNames();
        JsonNode resp = get("/environments/" + environmentClientKey + "/featurestates/", adminToken);
        List<FeatureStateInfo> result = new ArrayList<>();
        for (JsonNode fs : asArray(resp)) {
            result.add(parseFeatureState(fs, featureNames));
        }
        return result;
    }

    public FeatureStateInfo updateFeatureState(long featureStateId, Boolean enabled, String value) {
        ensureDiscovered();
        ObjectNode body = mapper.createObjectNode();
        if (enabled != null) {
            body.put("enabled", enabled);
        }
        if (value != null) {
            // Empty string = clear to null; non-empty = set value
            if (value.isEmpty()) {
                body.putNull("feature_state_value");
            } else {
                body.put("feature_state_value", value);
            }
        }
        // PATCH returns a sparse response, so re-fetch the full state afterwards
        patch("/environments/" + environmentClientKey + "/featurestates/" + featureStateId + "/", body, adminToken);
        JsonNode full = get("/environments/" + environmentClientKey + "/featurestates/" + featureStateId + "/", adminToken);
        Map<Long, String> featureNames = loadFeatureNames();
        return parseFeatureState(full, featureNames);
    }

    /**
     * Updates a segment override feature state via the top-level featurestates endpoint.
     * The environment-scoped endpoint only serves environment defaults, so segment
     * override states must be updated through /features/featurestates/{id}/.
     */
    public FeatureStateInfo updateSegmentOverrideState(long featureStateId, Boolean enabled, String value) {
        ensureDiscovered();

        // The top-level /features/featurestates/ uses FeatureStateSerializerFull which
        // needs a complete body via PUT. We build it from the environment document.
        JsonNode currentFs = findFeatureStateInEnvDocument(featureStateId);
        if (currentFs == null) {
            throw new IllegalStateException("Feature state %d not found in environment document".formatted(featureStateId));
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("id", featureStateId);
        body.put("feature", currentFs.get("feature").get("id").asLong());
        body.put("environment", environmentId);
        body.put("enabled", enabled != null ? enabled : currentFs.get("enabled").asBoolean());

        // Resolve feature_segment link ID
        long featureId = currentFs.get("feature").get("id").asLong();
        var fsLinkId = findFeatureSegmentId(featureId, findSegmentIdForFeatureState(featureStateId));
        if (fsLinkId.isPresent()) {
            body.put("feature_segment", fsLinkId.get());
        }

        // Build feature_state_value as nested dict
        ObjectNode fsv = mapper.createObjectNode();
        if (value != null && !value.isEmpty()) {
            fsv.put("type", "unicode");
            fsv.put("string_value", value);
            fsv.putNull("boolean_value");
            fsv.putNull("integer_value");
        } else {
            // Preserve current value if not changing
            Object currentVal = extractValue(currentFs);
            if (value != null && value.isEmpty()) {
                // Explicitly clearing value
                fsv.put("type", "unicode");
                fsv.putNull("string_value");
                fsv.putNull("boolean_value");
                fsv.putNull("integer_value");
            } else if (currentVal instanceof String sv) {
                fsv.put("type", "unicode");
                fsv.put("string_value", sv);
                fsv.putNull("boolean_value");
                fsv.putNull("integer_value");
            } else if (currentVal instanceof Number nv) {
                fsv.put("type", "int");
                fsv.putNull("string_value");
                fsv.putNull("boolean_value");
                fsv.put("integer_value", nv.longValue());
            } else if (currentVal instanceof Boolean bv) {
                fsv.put("type", "bool");
                fsv.putNull("string_value");
                fsv.put("boolean_value", bv);
                fsv.putNull("integer_value");
            } else {
                fsv.put("type", "unicode");
                fsv.putNull("string_value");
                fsv.putNull("boolean_value");
                fsv.putNull("integer_value");
            }
        }
        body.set("feature_state_value", fsv);

        JsonNode resp = put("/features/featurestates/" + featureStateId + "/", body, adminToken);
        Map<Long, String> featureNames = loadFeatureNames();
        return parseFeatureState(resp, featureNames);
    }

    /**
     * Finds a feature state node in the environment document by its django_id/id.
     */
    private JsonNode findFeatureStateInEnvDocument(long featureStateId) {
        JsonNode doc = getEnvironmentDocument();
        JsonNode project = doc.get("project");
        if (project == null) return null;
        JsonNode segments = project.get("segments");
        if (segments == null) return null;
        for (JsonNode segment : segments) {
            JsonNode featureStates = segment.get("feature_states");
            if (featureStates == null) continue;
            for (JsonNode fs : featureStates) {
                long id = fs.has("django_id") && !fs.get("django_id").isNull()
                        ? fs.get("django_id").asLong() : fs.get("id").asLong();
                if (id == featureStateId) return fs;
            }
        }
        return null;
    }

    /**
     * Finds which segment a feature state belongs to (from env document).
     */
    private long findSegmentIdForFeatureState(long featureStateId) {
        JsonNode doc = getEnvironmentDocument();
        JsonNode project = doc.get("project");
        if (project == null) throw new IllegalStateException("No project in env document");
        JsonNode segments = project.get("segments");
        if (segments == null) throw new IllegalStateException("No segments in env document");
        for (JsonNode segment : segments) {
            JsonNode featureStates = segment.get("feature_states");
            if (featureStates == null) continue;
            for (JsonNode fs : featureStates) {
                long id = fs.has("django_id") && !fs.get("django_id").isNull()
                        ? fs.get("django_id").asLong() : fs.get("id").asLong();
                if (id == featureStateId) return segment.get("id").asLong();
            }
        }
        throw new IllegalStateException("Feature state %d not found in any segment".formatted(featureStateId));
    }

    public Optional<FeatureStateInfo> findFeatureStateByName(String flagName) {
        return listFeatureStates().stream()
                .filter(fs -> fs.featureName().equals(flagName))
                .findFirst();
    }

    /**
     * Loads feature ID → name mapping from the project features endpoint.
     */
    private Map<Long, String> loadFeatureNames() {
        JsonNode resp = get("/projects/" + projectId + "/features/", adminToken);
        Map<Long, String> names = new HashMap<>();
        for (JsonNode f : asArray(resp)) {
            names.put(f.get("id").asLong(), text(f, "name"));
        }
        return names;
    }

    // --- Identity operations ---

    /**
     * Finds an identity by its identifier string. Returns empty if the identity
     * has never been evaluated (i.e. doesn't exist in Flagsmith yet).
     */
    public Optional<IdentityInfo> findIdentity(String identityString) {
        ensureDiscovered();
        JsonNode resp = get("/environments/" + environmentClientKey + "/identities/?identifier=" + encode(identityString), adminToken);
        JsonNode results = asArray(resp);
        for (JsonNode node : results) {
            if (identityString.equals(text(node, "identifier"))) {
                return Optional.of(new IdentityInfo(node.get("id").asLong(), identityString));
            }
        }
        return Optional.empty();
    }

    public List<IdentityOverrideInfo> listIdentityOverrides(long identityId) {
        ensureDiscovered();
        JsonNode resp = get("/environments/" + environmentClientKey + "/identities/" + identityId + "/featurestates/", adminToken);
        List<IdentityOverrideInfo> result = new ArrayList<>();
        for (JsonNode fs : asArray(resp)) {
            result.add(parseIdentityOverride(fs));
        }
        return result;
    }

    public IdentityOverrideInfo createIdentityOverride(long identityId, long featureId, boolean enabled, String value) {
        ensureDiscovered();
        ObjectNode body = mapper.createObjectNode();
        body.put("feature", featureId);
        body.put("enabled", enabled);
        if (value != null) {
            body.put("feature_state_value", value);
        } else {
            body.putNull("feature_state_value");
        }
        JsonNode resp = post("/environments/" + environmentClientKey + "/identities/" + identityId + "/featurestates/", body, adminToken);
        return parseIdentityOverride(resp);
    }

    public IdentityOverrideInfo updateIdentityOverride(long identityId, long featureStateId, Boolean enabled, String value) {
        ensureDiscovered();
        ObjectNode body = mapper.createObjectNode();
        if (enabled != null) {
            body.put("enabled", enabled);
        }
        if (value != null) {
            body.put("feature_state_value", value);
        }
        JsonNode resp = patch("/environments/" + environmentClientKey + "/identities/" + identityId + "/featurestates/" + featureStateId + "/", body, adminToken);
        return parseIdentityOverride(resp);
    }

    public void deleteIdentityOverride(long identityId, long featureStateId) {
        ensureDiscovered();
        delete("/environments/" + environmentClientKey + "/identities/" + identityId + "/featurestates/" + featureStateId + "/", adminToken);
    }

    // --- Segment operations ---

    /**
     * Lists all segments defined in the project.
     */
    public List<SegmentInfo> listSegments() {
        ensureDiscovered();
        JsonNode resp = get("/projects/" + projectId + "/segments/", adminToken);
        List<SegmentInfo> result = new ArrayList<>();
        for (JsonNode s : asArray(resp)) {
            result.add(new SegmentInfo(
                    s.get("id").asLong(),
                    text(s, "name"),
                    text(s, "description")
            ));
        }
        return result;
    }

    /**
     * Fetches all segments and their feature flag overrides using the Admin API.
     * For each feature, queries its feature-segment links, then resolves the
     * corresponding feature state values.
     */
    /**
     * Fetches all segments and their feature flag overrides using the
     * environment document (same data the SDK uses for local evaluation).
     * Segment descriptions are enriched from the admin segments endpoint.
     */
    public List<SegmentWithOverrides> getSegmentOverrides() {
        ensureDiscovered();

        JsonNode doc = getEnvironmentDocument();
        JsonNode project = doc.get("project");
        if (project == null) return List.of();
        JsonNode segments = project.get("segments");
        if (segments == null || !segments.isArray()) return List.of();

        // Fetch segment descriptions (env doc doesn't include them)
        Map<Long, String> segmentDescriptions = new HashMap<>();
        try {
            for (SegmentInfo info : listSegments()) {
                segmentDescriptions.put(info.id(), info.description());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch segment descriptions: {}", e.getMessage());
        }

        List<SegmentWithOverrides> result = new ArrayList<>();
        for (JsonNode segment : segments) {
            long segmentId = segment.get("id").asLong();
            String segmentName = text(segment, "name");

            List<SegmentOverrideState> overrides = new ArrayList<>();
            JsonNode featureStates = segment.get("feature_states");
            if (featureStates != null && featureStates.isArray()) {
                for (JsonNode fs : featureStates) {
                    long featureStateId = fs.has("django_id") && !fs.get("django_id").isNull()
                            ? fs.get("django_id").asLong()
                            : fs.get("id").asLong();
                    JsonNode feature = fs.get("feature");
                    long featureId = feature.get("id").asLong();
                    String featureName = text(feature, "name");
                    boolean enabled = fs.get("enabled").asBoolean();
                    Object value = extractValue(fs);

                    overrides.add(new SegmentOverrideState(
                            featureStateId, featureId, featureName, enabled, value));
                }
            }

            result.add(new SegmentWithOverrides(
                    segmentId, segmentName,
                    segmentDescriptions.get(segmentId),
                    overrides));
        }
        return result;
    }

    /**
     * Creates a feature-segment link (segment override) for a feature.
     * Flagsmith auto-creates a FeatureState with environment defaults.
     */
    public long createFeatureSegment(long featureId, long segmentId) {
        ensureDiscovered();
        ObjectNode body = mapper.createObjectNode();
        body.put("feature", featureId);
        body.put("segment", segmentId);
        body.put("environment", environmentId);
        JsonNode resp = post("/features/feature-segments/", body, adminToken);
        return resp.get("id").asLong();
    }

    /**
     * Finds the feature-segment link ID for a given feature + segment combo.
     */
    public Optional<Long> findFeatureSegmentId(long featureId, long segmentId) {
        ensureDiscovered();
        JsonNode resp = get("/features/feature-segments/?feature=" + featureId + "&environment=" + environmentId, adminToken);
        for (JsonNode fs : asArray(resp)) {
            if (fs.get("segment").asLong() == segmentId) {
                return Optional.of(fs.get("id").asLong());
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the feature state ID for a segment override from the
     * environment document, matching by segment + feature.
     */
    public Optional<Long> findSegmentOverrideFeatureStateId(long segmentId, long featureId) {
        ensureDiscovered();
        JsonNode doc = getEnvironmentDocument();
        JsonNode project = doc.get("project");
        if (project == null) return Optional.empty();
        JsonNode segments = project.get("segments");
        if (segments == null) return Optional.empty();

        for (JsonNode segment : segments) {
            if (segment.get("id").asLong() != segmentId) continue;
            JsonNode featureStates = segment.get("feature_states");
            if (featureStates == null) continue;
            for (JsonNode fs : featureStates) {
                JsonNode feature = fs.get("feature");
                if (feature.get("id").asLong() == featureId) {
                    return Optional.of(
                            fs.has("django_id") && !fs.get("django_id").isNull()
                                    ? fs.get("django_id").asLong()
                                    : fs.get("id").asLong());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Deletes a feature-segment link, cascading the associated feature state.
     */
    public void deleteFeatureSegment(long featureSegmentId) {
        ensureDiscovered();
        delete("/features/feature-segments/" + featureSegmentId + "/", adminToken);
    }

    /**
     * Builds the Flagsmith identity string for a user in a team,
     * matching the format used by {@link FeatureFlagService#buildIdentity}.
     */
    public static String buildIdentity(String teamIdentifier, String userIdentifier) {
        return "team:%s_user:%s".formatted(teamIdentifier, userIdentifier);
    }

    // --- Internal: environment document ---

    private JsonNode getEnvironmentDocument() {
        if (serverSideKey == null) {
            throw new IllegalStateException("Server-side key not available — cannot fetch environment document");
        }
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/environment-document/"))
                    .header("X-Environment-Key", serverSideKey)
                    .GET()
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Environment document request returned %d: %s".formatted(resp.statusCode(), resp.body()));
            }
            return mapper.readTree(resp.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch environment document: " + e.getMessage(), e);
        }
    }

    // --- Internal: auth & discovery ---

    private void ensureDiscovered() {
        ensureAuthenticated();
        if (projectId == null || environmentClientKey == null) {
            discover();
        }
    }

    private void ensureAuthenticated() {
        if (adminToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return;
        }
        login();
    }

    private void login() {
        if (properties.adminEmail() == null || properties.adminPassword() == null) {
            throw new IllegalStateException("Flagsmith admin credentials not configured");
        }
        ObjectNode body = mapper.createObjectNode()
                .put("email", properties.adminEmail())
                .put("password", properties.adminPassword());
        JsonNode resp = post("/auth/login/", body, null);
        if (!resp.has("key")) {
            throw new IllegalStateException("Flagsmith admin login failed");
        }
        this.adminToken = resp.get("key").asText();
        this.tokenExpiresAt = Instant.now().plus(TOKEN_TTL);
        log.debug("Flagsmith admin token refreshed");
    }

    private void discover() {
        // Find project
        JsonNode projects = get("/projects/", adminToken);
        for (JsonNode p : asArray(projects)) {
            if (properties.projectName().equals(text(p, "name"))) {
                this.projectId = p.get("id").asInt();
                break;
            }
        }
        if (projectId == null) {
            throw new IllegalStateException("Flagsmith project '%s' not found".formatted(properties.projectName()));
        }

        // Find environment client key and ID
        JsonNode envs = get("/environments/?project=" + projectId, adminToken);
        for (JsonNode env : asArray(envs)) {
            if (properties.environmentName().equals(text(env, "name"))) {
                this.environmentClientKey = env.get("api_key").asText();
                this.environmentId = env.get("id").asInt();
                break;
            }
        }
        if (environmentClientKey == null) {
            throw new IllegalStateException("Flagsmith environment '%s' not found".formatted(properties.environmentName()));
        }

        // Find or create server-side key (needed for environment-document endpoint)
        String apiKeysPath = "/environments/" + environmentClientKey + "/api-keys/";
        JsonNode keys = get(apiKeysPath, adminToken);
        for (JsonNode k : asArray(keys)) {
            if (SERVER_KEY_NAME.equals(text(k, "name"))) {
                this.serverSideKey = k.get("key").asText();
                break;
            }
        }
        if (serverSideKey == null) {
            // Create it if it doesn't exist
            ObjectNode keyBody = mapper.createObjectNode().put("name", SERVER_KEY_NAME);
            JsonNode created = post(apiKeysPath, keyBody, adminToken);
            if (created.has("key")) {
                this.serverSideKey = created.get("key").asText();
            }
        }

        log.info("Flagsmith admin discovery: project={}, environmentId={}, environmentKey={}, serverKey={}", projectId, environmentId, environmentClientKey, serverSideKey != null ? "resolved" : "MISSING");
    }

    // --- Internal: HTTP helpers ---

    private JsonNode get(String path, String token) {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Token " + token)
                    .GET()
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp, "GET", path);
            return mapper.readTree(resp.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Flagsmith API GET %s failed: %s".formatted(path, e.getMessage()), e);
        }
    }

    private JsonNode post(String path, JsonNode payload, String token) {
        try {
            var builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .timeout(REQUEST_TIMEOUT);
            if (token != null) {
                builder.header("Authorization", "Token " + token);
            }
            HttpResponse<String> resp = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            checkStatus(resp, "POST", path);
            return mapper.readTree(resp.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Flagsmith API POST %s failed: %s".formatted(path, e.getMessage()), e);
        }
    }

    private JsonNode put(String path, JsonNode payload, String token) {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Token " + token)
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp, "PUT", path);
            return mapper.readTree(resp.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Flagsmith API PUT %s failed: %s".formatted(path, e.getMessage()), e);
        }
    }

    private JsonNode patch(String path, JsonNode payload, String token) {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Token " + token)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp, "PATCH", path);
            return mapper.readTree(resp.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Flagsmith API PATCH %s failed: %s".formatted(path, e.getMessage()), e);
        }
    }

    private void delete(String path, String token) {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Token " + token)
                    .DELETE()
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            checkStatus(resp, "DELETE", path);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Flagsmith API DELETE %s failed: %s".formatted(path, e.getMessage()), e);
        }
    }

    private void checkStatus(HttpResponse<String> resp, String method, String path) {
        int status = resp.statusCode();
        if (status >= 200 && status < 300) return;
        if (status == 401) {
            // Token expired — clear cached token so next call re-authenticates
            this.adminToken = null;
            this.tokenExpiresAt = Instant.MIN;
        }
        throw new IllegalStateException("Flagsmith API %s %s returned %d: %s".formatted(method, path, status, resp.body()));
    }

    // --- Internal: parsing ---

    private FeatureStateInfo parseFeatureState(JsonNode fs, Map<Long, String> featureNames) {
        long featureId = resolveFeatureId(fs);
        return new FeatureStateInfo(
                fs.get("id").asLong(),
                featureId,
                featureNames.getOrDefault(featureId, "unknown"),
                fs.get("enabled").asBoolean(),
                extractValue(fs)
        );
    }

    private IdentityOverrideInfo parseIdentityOverride(JsonNode fs) {
        long featureId = resolveFeatureId(fs);
        JsonNode featureNode = fs.get("feature");
        String featureName = (featureNode != null && featureNode.isObject()) ? text(featureNode, "name") : null;
        return new IdentityOverrideInfo(
                fs.get("id").asLong(),
                featureId,
                featureName,
                fs.get("enabled").asBoolean(),
                extractValue(fs)
        );
    }

    /** Handles both flat integer and nested object forms of the feature field. */
    private long resolveFeatureId(JsonNode fs) {
        JsonNode featureNode = fs.get("feature");
        if (featureNode == null) throw new IllegalStateException("Missing 'feature' field in feature state");
        return featureNode.isObject() ? featureNode.get("id").asLong() : featureNode.asLong();
    }

    private Object extractValue(JsonNode fs) {
        JsonNode val = fs.get("feature_state_value");
        if (val == null || val.isNull()) return null;
        // The top-level /features/featurestates/ endpoint returns feature_state_value
        // as a nested dict: {"type": "unicode", "string_value": "...", ...}
        if (val.isObject()) {
            String type = text(val, "type");
            if ("unicode".equals(type)) {
                JsonNode sv = val.get("string_value");
                return (sv == null || sv.isNull()) ? null : sv.asText();
            } else if ("int".equals(type)) {
                JsonNode iv = val.get("integer_value");
                return (iv == null || iv.isNull()) ? null : iv.numberValue();
            } else if ("bool".equals(type)) {
                JsonNode bv = val.get("boolean_value");
                return (bv == null || bv.isNull()) ? null : bv.booleanValue();
            }
            return null;
        }
        if (val.isTextual()) return val.asText();
        if (val.isNumber()) return val.numberValue();
        if (val.isBoolean()) return val.booleanValue();
        return val.asText();
    }

    private static String resolveBaseUrl(String apiUrl) {
        String url = apiUrl.replaceAll("/+$", "");
        return url.contains("/api/v1") ? url : url + "/api/v1";
    }

    private static JsonNode asArray(JsonNode node) {
        if (node.isArray()) return node;
        if (node.has("results")) return node.get("results");
        throw new IllegalStateException("Unexpected Flagsmith response format");
    }

    private static String text(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
