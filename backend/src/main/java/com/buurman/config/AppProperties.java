package com.buurman.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String version,
        Email email,
        Api api,
        Cors cors,
        Documents documents
) {
    public record Email(
            String from,
            String fromName,
            String baseUrl
    ) {}

    public record Api(
            String baseUrl
    ) {}

    public record Cors(
            List<String> allowedOrigins,
            List<String> backofficeAllowedOrigins
    ) {}

    public record Documents(
            long maxFileSize,
            List<String> allowedMimeTypes
    ) {}
}
