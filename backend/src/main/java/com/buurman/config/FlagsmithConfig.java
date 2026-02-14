package com.buurman.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flagsmith.FlagsmithClient;
import com.flagsmith.models.DefaultFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Configuration
public class FlagsmithConfig {

    private static final Logger log = LoggerFactory.getLogger(FlagsmithConfig.class);

    private final FlagsmithProperties properties;

    public FlagsmithConfig(FlagsmithProperties properties) {
        this.properties = properties;
    }

    @Bean
    public FlagsmithClient flagsmithClient() {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Flagsmith API key not available — feature flags disabled");
            return null;
        }

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

    private String resolveApiKey() {
        // 1. Explicit key from property / env var
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            return properties.apiKey();
        }

        // 2. Auto-discover from Flagsmith admin API
        if (properties.adminEmail() != null && properties.adminPassword() != null) {
            return discoverApiKey();
        }

        return null;
    }

    /**
     * Discovers the server-side environment key by logging into Flagsmith's admin API
     * and looking up the project/environment. Same flow as the setup script.
     */
    private String discoverApiKey() {
        String baseUrl = properties.apiUrl().replaceAll("/+$", "");
        // The apiUrl includes /api/v1/ already, strip it for the admin auth endpoints
        String adminBase = baseUrl.contains("/api/v1") ? baseUrl : baseUrl + "/api/v1";

        String projectName = properties.projectName() != null ? properties.projectName() : "Buurman";
        String envName = properties.environmentName() != null ? properties.environmentName() : "Local";

        log.info("Discovering Flagsmith API key (project='{}', env='{}')", projectName, envName);

        try {
            var mapper = new ObjectMapper();
            var httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // Login
            String loginBody = mapper.writeValueAsString(
                    mapper.createObjectNode()
                            .put("email", properties.adminEmail())
                            .put("password", properties.adminPassword())
            );

            HttpResponse<String> loginResp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(adminBase + "/auth/login/"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                            .timeout(Duration.ofSeconds(10))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode loginJson = mapper.readTree(loginResp.body());
            String token = loginJson.has("key") ? loginJson.get("key").asText() : null;
            if (token == null) {
                log.warn("Flagsmith auto-discovery: login failed ({})", loginResp.body());
                return null;
            }

            // Find project
            HttpResponse<String> projResp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(adminBase + "/projects/"))
                            .header("Authorization", "Token " + token)
                            .GET()
                            .timeout(Duration.ofSeconds(10))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode projects = mapper.readTree(projResp.body());
            if (!projects.isArray()) {
                log.warn("Flagsmith auto-discovery: unexpected projects response");
                return null;
            }

            Integer projectId = null;
            for (JsonNode p : projects) {
                if (projectName.equals(p.get("name").asText())) {
                    projectId = p.get("id").asInt();
                    break;
                }
            }
            if (projectId == null) {
                log.warn("Flagsmith auto-discovery: project '{}' not found", projectName);
                return null;
            }

            // Find environment
            HttpResponse<String> envResp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(adminBase + "/environments/?project=" + projectId))
                            .header("Authorization", "Token " + token)
                            .GET()
                            .timeout(Duration.ofSeconds(10))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode envsJson = mapper.readTree(envResp.body());
            JsonNode envList = envsJson.has("results") ? envsJson.get("results") : envsJson;
            if (!envList.isArray()) {
                log.warn("Flagsmith auto-discovery: unexpected environments response");
                return null;
            }

            String clientKey = null;
            for (JsonNode env : envList) {
                if (envName.equals(env.get("name").asText())) {
                    clientKey = env.get("api_key").asText();
                    break;
                }
            }

            if (clientKey == null) {
                log.warn("Flagsmith auto-discovery: environment '{}' not found in project '{}'", envName, projectName);
                return null;
            }

            // Fetch server-side API key (the client-side key cannot be used by server SDKs)
            String serverKeyName = "Backend";
            HttpResponse<String> keysResp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(adminBase + "/environments/" + clientKey + "/api-keys/"))
                            .header("Authorization", "Token " + token)
                            .GET()
                            .timeout(Duration.ofSeconds(10))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode keysJson = mapper.readTree(keysResp.body());
            JsonNode keysList = keysJson.isArray() ? keysJson : keysJson.has("results") ? keysJson.get("results") : mapper.createArrayNode();

            for (JsonNode k : keysList) {
                if (serverKeyName.equals(k.get("name").asText())) {
                    String serverKey = k.get("key").asText();
                    log.info("Flagsmith auto-discovery: resolved server-side key for env '{}'", envName);
                    return serverKey;
                }
            }

            // Server-side key not found — create one
            log.info("Flagsmith auto-discovery: creating server-side key '{}'", serverKeyName);
            String createBody = mapper.writeValueAsString(
                    mapper.createObjectNode().put("name", serverKeyName)
            );
            HttpResponse<String> createResp = httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(adminBase + "/environments/" + clientKey + "/api-keys/"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Token " + token)
                            .POST(HttpRequest.BodyPublishers.ofString(createBody))
                            .timeout(Duration.ofSeconds(10))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            JsonNode created = mapper.readTree(createResp.body());
            if (created.has("key")) {
                String serverKey = created.get("key").asText();
                log.info("Flagsmith auto-discovery: created server-side key for env '{}'", envName);
                return serverKey;
            }

            log.warn("Flagsmith auto-discovery: failed to create server-side key: {}", createResp.body());
            return null;
        } catch (Exception e) {
            log.warn("Flagsmith auto-discovery failed: {}", e.getMessage());
            return null;
        }
    }

    private static DefaultFlag defaultFlagHandler(String featureName) {
        DefaultFlag flag = new DefaultFlag();
        flag.setEnabled(false);
        flag.setValue(null);
        return flag;
    }
}
