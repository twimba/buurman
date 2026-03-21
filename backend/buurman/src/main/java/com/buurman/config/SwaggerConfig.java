package com.buurman.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.buurman.security.SwaggerAccessFilter;
import com.buurman.service.FeatureFlagService;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.annotation.PostConstruct;
import com.buurman.util.Generated;

@Configuration
@Generated
public class SwaggerConfig {

  @PostConstruct
  public void configureSwaggerMapper() {
    Jdk8Module jdk8Module = new Jdk8Module();
    Json.mapper().registerModule(jdk8Module);
    Json31.mapper().registerModule(jdk8Module);
  }

  @Bean
  public FilterRegistrationBean<SwaggerAccessFilter> swaggerAccessFilter(
      FeatureFlagService featureFlagService) {
    FilterRegistrationBean<SwaggerAccessFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new SwaggerAccessFilter(featureFlagService));
    registration.addUrlPatterns("/api-docs/*", "/swagger-ui/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Bean
  public OpenAPI buurmanOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Buurman API")
                .description("Property Management API for Small Landlords")
                .version("0.1.0")
                .contact(new Contact().name("Buurman Team").email("info@buurman.io")))
        .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearer-jwt",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
