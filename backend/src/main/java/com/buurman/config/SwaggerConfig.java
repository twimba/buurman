package com.buurman.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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
                                .email("info@buurman.com")));
    }
}
