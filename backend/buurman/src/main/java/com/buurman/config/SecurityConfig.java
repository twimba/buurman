package com.buurman.config;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.TeamRole;
import com.buurman.security.EmailVerificationFilter;
import com.buurman.security.JwtAuthenticationConverter;
import com.buurman.security.MdcFilter;
import com.buurman.security.RateLimitFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationConverter jwtAuthenticationConverter;
  private final MdcFilter mdcFilter;
  private final EmailVerificationFilter emailVerificationFilter;
  private final RateLimitFilter rateLimitFilter;
  private final AppProperties appProperties;

  public SecurityConfig(
      JwtAuthenticationConverter jwtAuthenticationConverter,
      MdcFilter mdcFilter,
      EmailVerificationFilter emailVerificationFilter,
      RateLimitFilter rateLimitFilter,
      AppProperties appProperties) {
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    this.mdcFilter = mdcFilter;
    this.emailVerificationFilter = emailVerificationFilter;
    this.rateLimitFilter = rateLimitFilter;
    this.appProperties = appProperties;
  }

  @Bean
  @Order(0)
  public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/actuator/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  @Order(3)
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/health", "/info", "/reference/**")
                    .permitAll()
                    .requestMatchers("/api-docs/**", "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers(POST, "/auth/register")
                    .permitAll()
                    .requestMatchers(POST, "/registration-invitations/validate")
                    .permitAll()
                    .requestMatchers(GET, "/registration/config")
                    .permitAll()
                    .requestMatchers(GET, "/invitations/*")
                    .permitAll()
                    .requestMatchers(GET, "/auth/verify-email-token")
                    .permitAll()
                    .requestMatchers(GET, "/calendar/ical/*")
                    .permitAll()
                    .requestMatchers("/webhooks/**")
                    .permitAll()
                    .requestMatchers(GET, "/broadcasts/public")
                    .permitAll()
                    // All other endpoints require authentication
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .addFilterAfter(mdcFilter, BearerTokenAuthenticationFilter.class)
        .addFilterAfter(emailVerificationFilter, MdcFilter.class)
        .addFilterBefore(rateLimitFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(appProperties.cors().allowedOrigins());
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy(TeamRole.hierarchy());
  }
}
