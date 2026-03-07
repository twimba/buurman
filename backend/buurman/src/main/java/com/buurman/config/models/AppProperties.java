package com.buurman.config.models;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    String version, String publicUrl, Email email, Api api, Cors cors, Documents documents) {
  public record Email(String from, String fromName, String baseUrl) {}

  public record Api(String baseUrl) {}

  public record Cors(List<String> allowedOrigins, List<String> backofficeAllowedOrigins) {}

  public record Documents(long maxFileSize, List<String> allowedMimeTypes) {}
}
