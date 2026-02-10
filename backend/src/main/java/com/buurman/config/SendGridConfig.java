package com.buurman.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")
public class SendGridConfig {

    @Bean
    public SendGrid sendGrid(@Value("${sendgrid.api-key}") String apiKey) {
        return new SendGrid(apiKey);
    }
}
