package com.buurman.config;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    private final KeycloakProperties keycloakProperties;

    public KeycloakConfig(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
    }

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakProperties.admin().serverUrl())
                .realm(keycloakProperties.admin().realm())
                .clientId(keycloakProperties.admin().clientId())
                .username(keycloakProperties.admin().username())
                .password(keycloakProperties.admin().password())
                .resteasyClient(ResteasyClientBuilder.newBuilder().build())
                .build();
    }
}
