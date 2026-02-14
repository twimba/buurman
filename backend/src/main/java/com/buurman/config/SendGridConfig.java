package com.buurman.config;

import com.buurman.config.models.SendGridProperties;
import com.sendgrid.SendGrid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")
public class SendGridConfig {

    @Bean
    public SendGrid sendGrid(SendGridProperties sendGridProperties) {
        return new SendGrid(sendGridProperties.apiKey());
    }
}
