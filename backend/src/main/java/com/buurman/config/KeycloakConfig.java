package com.buurman.config;

import com.buurman.config.models.KeycloakProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class KeycloakConfig {

    private final KeycloakProperties keycloakProperties;


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
