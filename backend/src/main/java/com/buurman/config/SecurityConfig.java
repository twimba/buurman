package com.buurman.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final MdcFilter mdcFilter;
    private final DemoApiKeyFilter demoApiKeyFilter;
    private final EmailVerificationFilter emailVerificationFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter, MdcFilter mdcFilter,
                         DemoApiKeyFilter demoApiKeyFilter, EmailVerificationFilter emailVerificationFilter,
                         @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.mdcFilter = mdcFilter;
        this.demoApiKeyFilter = demoApiKeyFilter;
        this.emailVerificationFilter = emailVerificationFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/health", "/info", "/reference/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/invitations/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/calendar/ical/*").permitAll()
                .requestMatchers("/admin/demo-data/**").permitAll()
                .requestMatchers("/webhooks/**").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            )
            .addFilterAfter(mdcFilter, BearerTokenAuthenticationFilter.class)
            .addFilterAfter(emailVerificationFilter, MdcFilter.class)
            .addFilterBefore(demoApiKeyFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "X-Demo-Api-Key"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_TEAM_ADMIN > ROLE_TEAM_EDITOR > ROLE_TEAM_VIEWER");
    }
}
