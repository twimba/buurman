package com.buurman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BuurmanApplication {
    public static void main(String[] args) {
        SpringApplication.run(BuurmanApplication.class, args);
    }
}
