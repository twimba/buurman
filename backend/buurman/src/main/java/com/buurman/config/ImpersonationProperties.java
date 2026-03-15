package com.buurman.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "impersonation")
public record ImpersonationProperties(
    String jwtSecret,
    Duration maxTimeout,
    Duration defaultTimeout,
    Duration reauthWindow,
    String appBaseUrl) {

  public ImpersonationProperties {
    if (jwtSecret == null || jwtSecret.length() < 32) {
      throw new IllegalStateException(
          "Impersonation JWT secret must be at least 32 characters long");
    }
    if (maxTimeout == null || maxTimeout.isZero() || maxTimeout.isNegative()) {
      maxTimeout = Duration.ofMinutes(60);
    }
    if (defaultTimeout == null || defaultTimeout.isZero() || defaultTimeout.isNegative()) {
      defaultTimeout = Duration.ofMinutes(15);
    }
    if (reauthWindow == null || reauthWindow.isZero() || reauthWindow.isNegative()) {
      reauthWindow = Duration.ofMinutes(2);
    }
  }

  /** Returns the HMAC signing key derived from the JWT secret. */
  public javax.crypto.SecretKey signingKey() {
    return new javax.crypto.spec.SecretKeySpec(
        jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
  }
}
