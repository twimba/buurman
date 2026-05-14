package com.buurman.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.buurman.config.models.MailgunProperties;
import com.buurman.util.SkipTestCoverage;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;

@Configuration
@Profile("!local")
@SkipTestCoverage
public class MailgunConfig {

  @Bean
  public MailgunMessagesApi mailgunMessagesApi(MailgunProperties mailgunProperties) {
    String baseUrl =
        mailgunProperties.euRegion() ? "https://api.eu.mailgun.net" : "https://api.mailgun.net";
    return MailgunClient.config(baseUrl, mailgunProperties.apiKey())
        .createApi(MailgunMessagesApi.class);
  }
}
