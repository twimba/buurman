package com.buurman;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BuurmanApplication {
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(BuurmanApplication.class);
    app.setApplicationStartup(new BufferingApplicationStartup(4096));
    app.run(args);
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
