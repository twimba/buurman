package com.buurman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BuurmanApplication {
    public static void main(String[] args) {
        SpringApplication.run(BuurmanApplication.class, args);
    }
}
