package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.consolidation")
public record NotificationConsolidationProperties(
    boolean enabled, EmailConsolidation email, SmsConsolidation sms) {

  public record EmailConsolidation(boolean enabled, int minGroupSize, int maxItemsPerDigest) {}

  public record SmsConsolidation(boolean enabled, int threshold) {}
}
