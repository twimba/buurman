package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

@ConfigurationProperties(prefix = "keycloak")
@SkipTestCoverage
public record KeycloakProperties(Admin admin, String realm, String backofficeRealm) {
  public record Admin(
      String serverUrl, String realm, String clientId, String username, String password) {}
}
