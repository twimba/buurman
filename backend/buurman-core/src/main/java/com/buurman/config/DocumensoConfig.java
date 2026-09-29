package com.buurman.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.buurman.config.models.DocumensoProperties;

@Configuration
public class DocumensoConfig {

  @Bean
  public RestClient documensoRestClient(DocumensoProperties properties) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(30));
    return RestClient.builder()
        .baseUrl(properties.baseUrl() + "/api/v2")
        .defaultHeader("Authorization", properties.apiKey())
        .requestFactory(factory)
        .build();
  }
}
