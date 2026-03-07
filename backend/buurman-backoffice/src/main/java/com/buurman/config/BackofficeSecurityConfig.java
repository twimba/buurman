package com.buurman.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.buurman.config.models.AppProperties;
import com.buurman.security.BackofficeJwtAuthenticationConverter;

@Configuration
public class BackofficeSecurityConfig {

  private final BackofficeJwtAuthenticationConverter backofficeJwtAuthenticationConverter;
  private final AppProperties appProperties;
  private final String backofficeJwkSetUri;
  private final String backofficeIssuerUri;

  public BackofficeSecurityConfig(
      BackofficeJwtAuthenticationConverter backofficeJwtAuthenticationConverter,
      AppProperties appProperties,
      @Value("${spring.security.oauth2.resourceserver.jwt.backoffice.jwk-set-uri:}")
          String backofficeJwkSetUri,
      @Value("${spring.security.oauth2.resourceserver.jwt.backoffice.issuer-uri:}")
          String backofficeIssuerUri) {
    this.backofficeJwtAuthenticationConverter = backofficeJwtAuthenticationConverter;
    this.appProperties = appProperties;
    this.backofficeJwkSetUri = backofficeJwkSetUri;
    this.backofficeIssuerUri = backofficeIssuerUri;
  }

  @Bean
  @Order(2)
  public SecurityFilterChain backofficeFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/backoffice/**")
        .cors(cors -> cors.configurationSource(backofficeCorsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/backoffice/**")
                    .permitAll()
                    .requestMatchers("/backoffice/api-docs/**", "/backoffice/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .hasRole("BACKOFFICE_ADMIN"))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt ->
                        jwt.decoder(backofficeJwtDecoder())
                            .jwtAuthenticationConverter(backofficeJwtAuthenticationConverter)));

    return http.build();
  }

  private CorsConfigurationSource backofficeCorsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(appProperties.cors().backofficeAllowedOrigins());
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/backoffice/**", configuration);
    return source;
  }

  private JwtDecoder backofficeJwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(backofficeJwkSetUri).build();
    if (!backofficeIssuerUri.isBlank()) {
      decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(backofficeIssuerUri));
    }
    return decoder;
  }
}
