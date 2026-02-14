package com.buurman.config;

import com.flagsmith.FlagsmithClient;
import com.flagsmith.models.DefaultFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
            log.warn("Flagsmith API key not configured (neither api-key nor api-key-file) — feature flags disabled");
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
        // Prefer explicit env var / property
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            return properties.apiKey();
        }

        // Fall back to reading from file (written by flagsmith-setup in Docker)
        if (properties.apiKeyFile() != null && !properties.apiKeyFile().isBlank()) {
            Path keyFile = Path.of(properties.apiKeyFile());
            if (Files.exists(keyFile)) {
                try {
                    String key = Files.readString(keyFile).trim();
                    if (!key.isBlank()) {
                        log.info("Read Flagsmith API key from file: {}", properties.apiKeyFile());
                        return key;
                    }
                } catch (IOException e) {
                    log.warn("Failed to read Flagsmith API key from {}: {}", properties.apiKeyFile(), e.getMessage());
                }
            } else {
                log.warn("Flagsmith API key file not found: {}", properties.apiKeyFile());
            }
        }

        return null;
    }

    private static DefaultFlag defaultFlagHandler(String featureName) {
        DefaultFlag flag = new DefaultFlag();
        flag.setEnabled(false);
        flag.setValue(null);
        return flag;
    }
}
