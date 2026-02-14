package com.buurman.config;

import com.flagsmith.FlagsmithClient;
import com.flagsmith.models.DefaultFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "flagsmith.api-key", matchIfMissing = false)
public class FlagsmithConfig {

    private static final Logger log = LoggerFactory.getLogger(FlagsmithConfig.class);

    private final FlagsmithProperties properties;

    public FlagsmithConfig(FlagsmithProperties properties) {
        this.properties = properties;
    }

    @Bean
    public FlagsmithClient flagsmithClient() {
        log.info("Initialising Flagsmith client (url={})", properties.apiUrl());

        var config = com.flagsmith.config.FlagsmithConfig.newBuilder()
                .baseUri(properties.apiUrl())
                .withEnableAnalytics(properties.enableAnalytics())
                .withEnvironmentRefreshIntervalSeconds(properties.environmentRefreshIntervalSeconds())
                .build();

        return FlagsmithClient.newBuilder()
                .setApiKey(properties.apiKey())
                .withConfiguration(config)
                .setDefaultFlagValueFunction(FlagsmithConfig::defaultFlagHandler)
                .build();
    }

    private static DefaultFlag defaultFlagHandler(String featureName) {
        DefaultFlag flag = new DefaultFlag();
        flag.setEnabled(false);
        flag.setValue(null);
        return flag;
    }
}
