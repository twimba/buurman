package com.buurman.config;

import com.buurman.config.models.FlagsmithProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flagsmith.FlagsmithClient;
import com.flagsmith.models.DefaultFlag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class FlagsmithConfig {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String SERVER_KEY_NAME = "Backend";

    private final FlagsmithProperties properties;

    @Bean
    public FlagsmithClient flagsmithClient() {
        return resolveServerKey()
                .map(this::buildClient)
                .orElse(null);
    }

    private Optional<String> resolveServerKey() {
        if (properties.serverSideKey() != null && !properties.serverSideKey().isBlank()) {
            log.info("Using configured Flagsmith server-side key");
            return Optional.of(properties.serverSideKey());
        }
        return discoverServerKey();
    }

    private FlagsmithClient buildClient(String apiKey) {
        log.info("Initialising Flagsmith client (url={})", properties.apiUrl());

        var config = com.flagsmith.config.FlagsmithConfig.newBuilder()
                .baseUri(properties.apiUrl())
                .withEnableAnalytics(properties.enableAnalytics())
                .withEnvironmentRefreshIntervalSeconds(properties.environmentRefreshIntervalSeconds())
                .build();

        return FlagsmithClient.newBuilder()
                .setApiKey(apiKey)
                .withConfiguration(config)
                .setDefaultFlagValueFunction(FlagsmithConfig::defaultFlagHandler)
                .build();
    }

    /**
     * Discovers the server-side environment key by logging into the Flagsmith
     * admin API and walking: login → project → environment → server-side key.
     * Returns empty if Flagsmith is unreachable or not yet set up — the app
     * boots normally with all feature flags defaulting to OFF.
     */
    private Optional<String> discoverServerKey() {
        if (properties.adminEmail() == null || properties.adminPassword() == null) {
            log.warn("Flagsmith admin credentials not configured — feature flags disabled");
            return Optional.empty();
        }

        try {
            var http = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
            var mapper = new ObjectMapper();
            var api = new FlagsmithAdminApi(http, mapper, resolveBaseUrl());

            String token = api.login(properties.adminEmail(), properties.adminPassword());
            int projectId = api.findProjectId(token, properties.projectName());
            String clientKey = api.findEnvironmentClientKey(token, projectId, properties.environmentName());
            String serverKey = api.findOrCreateServerKey(token, clientKey);

            log.info("Flagsmith auto-discovery: resolved server-side key for env '{}'", properties.environmentName());
            return Optional.of(serverKey);
        } catch (Exception e) {
            log.warn("Flagsmith auto-discovery failed — feature flags disabled: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private String resolveBaseUrl() {
        String url = properties.apiUrl().replaceAll("/+$", "");
        return url.contains("/api/v1") ? url : url + "/api/v1";
    }

    private static DefaultFlag defaultFlagHandler(String featureName) {
        DefaultFlag flag = new DefaultFlag();
        flag.setEnabled(false);
        flag.setValue(null);
        return flag;
    }

    /**
     * Thin wrapper around Flagsmith's admin REST API. Each method throws
     * on failure so the caller can handle everything in a single catch block.
     */
    private static class FlagsmithAdminApi {

        private final HttpClient http;
        private final ObjectMapper mapper;
        private final String baseUrl;

        FlagsmithAdminApi(HttpClient http, ObjectMapper mapper, String baseUrl) {
            this.http = http;
            this.mapper = mapper;
            this.baseUrl = baseUrl;
        }

        String login(String email, String password) throws Exception {
            var body = mapper.createObjectNode()
                    .put("email", email)
                    .put("password", password);

            JsonNode resp = post("/auth/login/", body, null);
            if (!resp.has("key")) {
                throw new IllegalStateException("login failed");
            }
            return resp.get("key").asText();
        }

        int findProjectId(String token, String projectName) throws Exception {
            JsonNode projects = get("/projects/", token);
            for (JsonNode p : asArray(projects)) {
                if (projectName.equals(text(p, "name"))) {
                    return p.get("id").asInt();
                }
            }
            throw new IllegalStateException("project '%s' not found".formatted(projectName));
        }

        String findEnvironmentClientKey(String token, int projectId, String envName) throws Exception {
            JsonNode envs = get("/environments/?project=" + projectId, token);
            for (JsonNode env : asArray(envs)) {
                if (envName.equals(text(env, "name"))) {
                    return env.get("api_key").asText();
                }
            }
            throw new IllegalStateException("environment '%s' not found".formatted(envName));
        }

        String findOrCreateServerKey(String token, String clientKey) throws Exception {
            String path = "/environments/" + clientKey + "/api-keys/";
            JsonNode keys = get(path, token);
            for (JsonNode k : asArray(keys)) {
                if (SERVER_KEY_NAME.equals(text(k, "name"))) {
                    return k.get("key").asText();
                }
            }

            var body = mapper.createObjectNode().put("name", SERVER_KEY_NAME);
            JsonNode created = post(path, body, token);
            if (!created.has("key")) {
                throw new IllegalStateException("failed to create server-side key");
            }
            return created.get("key").asText();
        }

        private JsonNode get(String path, String token) throws Exception {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Token " + token)
                    .GET()
                    .timeout(REQUEST_TIMEOUT)
                    .build();
            return mapper.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
        }

        private JsonNode post(String path, JsonNode payload, String token) throws Exception {
            var builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .timeout(REQUEST_TIMEOUT);
            if (token != null) {
                builder.header("Authorization", "Token " + token);
            }
            return mapper.readTree(http.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body());
        }

        private static JsonNode asArray(JsonNode node) {
            if (node.isArray()) {
                return node;
            }
            if (node.has("results")) {
                return node.get("results");
            }
            throw new IllegalStateException("unexpected response format");
        }

        private static String text(JsonNode node, String field) {
            return node.has(field) ? node.get(field).asText() : null;
        }
    }
}
