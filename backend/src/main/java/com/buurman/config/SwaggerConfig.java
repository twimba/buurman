package com.buurman.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI buurmanOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Buurman API")
                        .description("Property Management API for Small Landlords")
                        .version("0.1.0")
                        .contact(new Contact()
                                .name("Buurman Team")
                                .email("info@buurman.io")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi appApi() {
        return GroupedOpenApi.builder()
                .group("app")
                .pathsToExclude("/backoffice/**")
                .build();
    }

    @Bean
    public GroupedOpenApi backofficeApi() {
        return GroupedOpenApi.builder()
                .group("backoffice")
                .pathsToMatch("/backoffice/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(new Info()
                                .title("Buurman Backoffice API")
                                .description("Internal administration API for Buurman platform management")
                                .version("0.1.0")
                                .contact(new Contact()
                                        .name("Buurman Team")
                                        .email("info@buurman.io"))))
                .build();
    }
}
