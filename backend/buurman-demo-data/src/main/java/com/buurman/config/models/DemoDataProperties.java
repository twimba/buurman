package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;
import com.buurman.util.Generated;

@ConfigurationProperties(prefix = "buurman.demo")
@Generated
public record DemoDataProperties(boolean enabled, String cron) {}
