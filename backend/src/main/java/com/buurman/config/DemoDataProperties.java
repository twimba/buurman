package com.buurman.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "buurman.demo")
public class DemoDataProperties {

    private boolean enabled = false;
    private String apiKey = "demo-secret-key";
    private boolean autoRegenerate = false;
    private String cron = "0 0 */12 * * *";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isAutoRegenerate() {
        return autoRegenerate;
    }

    public void setAutoRegenerate(boolean autoRegenerate) {
        this.autoRegenerate = autoRegenerate;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}
