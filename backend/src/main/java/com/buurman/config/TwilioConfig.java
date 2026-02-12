package com.buurman.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")
public class TwilioConfig {

    private final TwilioProperties twilioProperties;

    public TwilioConfig(TwilioProperties twilioProperties) {
        this.twilioProperties = twilioProperties;
    }

    @PostConstruct
    public void init() {
        Twilio.init(twilioProperties.accountSid(), twilioProperties.authToken());
    }
}
