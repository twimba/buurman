package com.buurman.config;

import com.buurman.config.models.TwilioProperties;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")
@RequiredArgsConstructor
public class TwilioConfig {

    private final TwilioProperties twilioProperties;


    @PostConstruct
    public void init() {
        Twilio.init(twilioProperties.accountSid(), twilioProperties.authToken());
    }
}
