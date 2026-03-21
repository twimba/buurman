package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;
import com.buurman.util.Generated;

@ConfigurationProperties(prefix = "notification.consolidation")
@Generated
public record NotificationConsolidationProperties(
    boolean enabled, EmailConsolidation email, SmsConsolidation sms) {

  public record EmailConsolidation(boolean enabled, int minGroupSize, int maxItemsPerDigest) {}

  public record SmsConsolidation(boolean enabled, int threshold) {}
}
