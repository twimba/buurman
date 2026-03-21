package com.buurman;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import com.buurman.util.Generated;

@SpringBootApplication
@ConfigurationPropertiesScan
@Generated
public class BuurmanApplication {
  public static void main(String[] args) {
    SpringApplication.run(BuurmanApplication.class, args);
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
