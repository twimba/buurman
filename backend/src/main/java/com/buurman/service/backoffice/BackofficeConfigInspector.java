package com.buurman.service.backoffice;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.buurman.config.models.AppProperties;
import com.buurman.config.models.AwsS3Properties;
import com.buurman.config.models.FlagsmithProperties;
import com.buurman.config.models.GoogleMapsProperties;
import com.buurman.config.models.KeycloakProperties;
import com.buurman.config.models.NotificationOutboxProperties;
import com.buurman.config.models.SendGridProperties;
import com.buurman.config.models.TwilioProperties;
import com.buurman.dto.response.backoffice.BackofficeSystemInfoResponse.ConfigEntry;

@Service
public class BackofficeConfigInspector {

  private static final Set<String> SENSITIVE_KEYWORDS =
      Set.of("password", "secret", "key", "token", "sid", "credential", "apikey");

  private final AppProperties appProperties;
  private final Environment environment;
  private final KeycloakProperties keycloakProperties;
  private final AwsS3Properties awsS3Properties;
  private final ObjectProvider<TwilioProperties> twilioPropertiesProvider;
  private final ObjectProvider<SendGridProperties> sendGridPropertiesProvider;
  private final ObjectProvider<GoogleMapsProperties> googleMapsPropertiesProvider;
  private final ObjectProvider<FlagsmithProperties> flagsmithPropertiesProvider;
  private final ObjectProvider<NotificationOutboxProperties> notificationOutboxPropertiesProvider;

  public BackofficeConfigInspector(
      AppProperties appProperties,
      Environment environment,
      KeycloakProperties keycloakProperties,
      AwsS3Properties awsS3Properties,
      ObjectProvider<TwilioProperties> twilioPropertiesProvider,
      ObjectProvider<SendGridProperties> sendGridPropertiesProvider,
      ObjectProvider<GoogleMapsProperties> googleMapsPropertiesProvider,
      ObjectProvider<FlagsmithProperties> flagsmithPropertiesProvider,
      ObjectProvider<NotificationOutboxProperties> notificationOutboxPropertiesProvider) {
    this.appProperties = appProperties;
    this.environment = environment;
    this.keycloakProperties = keycloakProperties;
    this.awsS3Properties = awsS3Properties;
    this.twilioPropertiesProvider = twilioPropertiesProvider;
    this.sendGridPropertiesProvider = sendGridPropertiesProvider;
    this.googleMapsPropertiesProvider = googleMapsPropertiesProvider;
    this.flagsmithPropertiesProvider = flagsmithPropertiesProvider;
    this.notificationOutboxPropertiesProvider = notificationOutboxPropertiesProvider;
  }

  public List<ConfigEntry> getConfiguration() {
    List<ConfigEntry> entries = new ArrayList<>();

    addAppConfig(entries);
    addSpringConfig(entries);
    addDatabaseConfig(entries);
    addKeycloakConfig(entries);
    addS3Config(entries);
    addTwilioConfig(entries);
    addSendGridConfig(entries);
    addGoogleMapsConfig(entries);
    addFlagsmithConfig(entries);
    addNotificationOutboxConfig(entries);
    addMailConfig(entries);

    return entries;
  }

  private void addAppConfig(List<ConfigEntry> entries) {
    addEntry(entries, "App", "version", appProperties.version());
    if (appProperties.email() != null) {
      addEntry(entries, "App", "email.from", appProperties.email().from());
      addEntry(entries, "App", "email.fromName", appProperties.email().fromName());
      addEntry(entries, "App", "email.baseUrl", appProperties.email().baseUrl());
    }
    if (appProperties.api() != null) {
      addEntry(entries, "App", "api.baseUrl", appProperties.api().baseUrl());
    }
    if (appProperties.cors() != null) {
      addEntry(
          entries,
          "App",
          "cors.allowedOrigins",
          appProperties.cors().allowedOrigins() != null
              ? String.join(", ", appProperties.cors().allowedOrigins())
              : null);
    }
  }

  private void addSpringConfig(List<ConfigEntry> entries) {
    addEntry(
        entries, "Spring", "profiles.active", String.join(", ", environment.getActiveProfiles()));
    addEntry(entries, "Spring", "server.port", environment.getProperty("server.port"));
    addEntry(
        entries,
        "Spring",
        "management.server.port",
        environment.getProperty("management.server.port"));
  }

  private void addDatabaseConfig(List<ConfigEntry> entries) {
    addEntry(entries, "Database", "url", environment.getProperty("spring.datasource.url"));
    addEntry(
        entries, "Database", "username", environment.getProperty("spring.datasource.username"));
    addEntry(
        entries,
        "Database",
        "password",
        obfuscate("password", environment.getProperty("spring.datasource.password")));
  }

  private void addKeycloakConfig(List<ConfigEntry> entries) {
    addEntry(entries, "Keycloak", "admin.serverUrl", keycloakProperties.admin().serverUrl());
    addEntry(entries, "Keycloak", "admin.realm", keycloakProperties.admin().realm());
    addEntry(entries, "Keycloak", "admin.clientId", keycloakProperties.admin().clientId());
    addEntry(entries, "Keycloak", "admin.username", keycloakProperties.admin().username());
    addEntry(
        entries,
        "Keycloak",
        "admin.password",
        obfuscate("password", keycloakProperties.admin().password()));
    addEntry(entries, "Keycloak", "realm", keycloakProperties.realm());
    addEntry(entries, "Keycloak", "backofficeRealm", keycloakProperties.backofficeRealm());
  }

  private void addS3Config(List<ConfigEntry> entries) {
    addEntry(entries, "AWS S3", "endpoint", awsS3Properties.endpoint());
    addEntry(entries, "AWS S3", "region", awsS3Properties.region());
    addEntry(entries, "AWS S3", "accessKey", obfuscate("key", awsS3Properties.accessKey()));
    addEntry(entries, "AWS S3", "secretKey", obfuscate("secret", awsS3Properties.secretKey()));
    addEntry(entries, "AWS S3", "bucketName", awsS3Properties.bucketName());
    addEntry(
        entries, "AWS S3", "usePresignedUrls", String.valueOf(awsS3Properties.usePresignedUrls()));
  }

  private void addTwilioConfig(List<ConfigEntry> entries) {
    var twilio = twilioPropertiesProvider.getIfAvailable();
    if (twilio != null) {
      addEntry(entries, "Twilio", "accountSid", obfuscate("sid", twilio.accountSid()));
      addEntry(entries, "Twilio", "authToken", obfuscate("token", twilio.authToken()));
      addEntry(entries, "Twilio", "fromNumber", twilio.fromNumber());
      addEntry(
          entries, "Twilio", "messagingServiceSid", obfuscate("sid", twilio.messagingServiceSid()));
    }
  }

  private void addSendGridConfig(List<ConfigEntry> entries) {
    var sg = sendGridPropertiesProvider.getIfAvailable();
    if (sg != null) {
      addEntry(entries, "SendGrid", "apiKey", obfuscate("key", sg.apiKey()));
      addEntry(entries, "SendGrid", "fromEmail", sg.fromEmail());
      addEntry(entries, "SendGrid", "fromName", sg.fromName());
    }
  }

  private void addGoogleMapsConfig(List<ConfigEntry> entries) {
    var gm = googleMapsPropertiesProvider.getIfAvailable();
    if (gm != null) {
      addEntry(entries, "Google Maps", "apiKey", obfuscate("key", gm.apiKey()));
    }
  }

  private void addFlagsmithConfig(List<ConfigEntry> entries) {
    var fs = flagsmithPropertiesProvider.getIfAvailable();
    if (fs != null) {
      addEntry(entries, "Flagsmith", "apiUrl", fs.apiUrl());
      addEntry(entries, "Flagsmith", "enableAnalytics", String.valueOf(fs.enableAnalytics()));
      addEntry(entries, "Flagsmith", "serverSideKey", obfuscate("key", fs.serverSideKey()));
      addEntry(entries, "Flagsmith", "adminPassword", obfuscate("password", fs.adminPassword()));
      addEntry(entries, "Flagsmith", "projectName", fs.projectName());
      addEntry(entries, "Flagsmith", "environmentName", fs.environmentName());
    }
  }

  private void addNotificationOutboxConfig(List<ConfigEntry> entries) {
    var outbox = notificationOutboxPropertiesProvider.getIfAvailable();
    if (outbox != null) {
      addEntry(entries, "Notification Outbox", "batchSize", String.valueOf(outbox.batchSize()));
      addEntry(entries, "Notification Outbox", "maxRetries", String.valueOf(outbox.maxRetries()));
    }
  }

  private void addMailConfig(List<ConfigEntry> entries) {
    addEntry(entries, "Mail", "host", environment.getProperty("spring.mail.host"));
    addEntry(entries, "Mail", "port", environment.getProperty("spring.mail.port"));
    addEntry(
        entries,
        "Mail",
        "password",
        obfuscate("password", environment.getProperty("spring.mail.password")));
  }

  private void addEntry(List<ConfigEntry> entries, String category, String key, String value) {
    entries.add(new ConfigEntry(category, key, value != null ? value : "\u2014"));
  }

  private String obfuscate(String fieldName, String value) {
    if (value == null || value.isBlank()) {
      return "\u2014";
    }
    String lowerField = fieldName.toLowerCase();
    for (String keyword : SENSITIVE_KEYWORDS) {
      if (lowerField.contains(keyword)) {
        return "********";
      }
    }
    return value;
  }
}
