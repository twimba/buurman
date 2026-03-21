package com.buurman.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.buurman.config.models.SendGridProperties;
import com.buurman.util.Generated;
import com.sendgrid.SendGrid;

@Configuration
@Profile("!local")
@Generated
public class SendGridConfig {

  @Bean
  public SendGrid sendGrid(SendGridProperties sendGridProperties) {
    return new SendGrid(sendGridProperties.apiKey());
  }
}
