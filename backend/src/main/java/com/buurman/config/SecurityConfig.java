package com.buurman.security;

import com.buurman.config.models.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final BackofficeJwtAuthenticationConverter backofficeJwtAuthenticationConverter;
    private final MdcFilter mdcFilter;
    private final EmailVerificationFilter emailVerificationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AppProperties appProperties;
    private final String backofficeJwkSetUri;
    private final String backofficeIssuerUri;

    public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter,
                         BackofficeJwtAuthenticationConverter backofficeJwtAuthenticationConverter,
                         MdcFilter mdcFilter,
                         EmailVerificationFilter emailVerificationFilter,
                         RateLimitFilter rateLimitFilter,
                         AppProperties appProperties,
                         @Value("${spring.security.oauth2.resourceserver.jwt.backoffice.jwk-set-uri:}") String backofficeJwkSetUri,
                         @Value("${spring.security.oauth2.resourceserver.jwt.backoffice.issuer-uri:}") String backofficeIssuerUri) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.backofficeJwtAuthenticationConverter = backofficeJwtAuthenticationConverter;
        this.mdcFilter = mdcFilter;
        this.emailVerificationFilter = emailVerificationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.appProperties = appProperties;
        this.backofficeJwkSetUri = backofficeJwkSetUri;
        this.backofficeIssuerUri = backofficeIssuerUri;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain backofficeFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/backoffice/**")
            .cors(cors -> cors.configurationSource(backofficeCorsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/backoffice/**").permitAll()
                .requestMatchers("/backoffice/api-docs/**", "/backoffice/swagger-ui/**").permitAll()
                .anyRequest().hasRole("BACKOFFICE_ADMIN")
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(backofficeJwtDecoder())
                    .jwtAuthenticationConverter(backofficeJwtAuthenticationConverter))
            );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/health", "/info", "/reference/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers(POST, "/auth/register").permitAll()
                .requestMatchers(POST, "/registration-invitations/validate").permitAll()
                .requestMatchers(GET, "/registration/config").permitAll()
                .requestMatchers(GET, "/invitations/*").permitAll()
                .requestMatchers(GET, "/calendar/ical/*").permitAll()
                .requestMatchers("/webhooks/**").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            )
            .addFilterAfter(mdcFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(emailVerificationFilter, MdcFilter.class)
            .addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return buildCorsSource(
                appProperties.cors().allowedOrigins(),
                "/**",
                Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
    }

    private CorsConfigurationSource backofficeCorsConfigurationSource() {
        return buildCorsSource(
                appProperties.cors().backofficeAllowedOrigins(),
                "/backoffice/**",
                Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
    }

    private CorsConfigurationSource buildCorsSource(List<String> origins, String pathPattern, List<String> allowedHeaders) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(pathPattern, configuration);
        return source;
    }

    private JwtDecoder backofficeJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(backofficeJwkSetUri).build();
        if (backofficeIssuerUri != null && !backofficeIssuerUri.isBlank()) {
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(backofficeIssuerUri));
        }
        return decoder;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_TEAM_ADMIN > ROLE_TEAM_EDITOR > ROLE_TEAM_VIEWER");
    }
}
