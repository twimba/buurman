package com.buurman.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.buurman.config.models.TwilioProperties;
import com.twilio.Twilio;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import com.buurman.util.Generated;

@Configuration
@Profile("!local")
@RequiredArgsConstructor
@Generated
public class TwilioConfig {

  private final TwilioProperties twilioProperties;

  @PostConstruct
  public void init() {
    Twilio.init(twilioProperties.accountSid(), twilioProperties.authToken());
  }
}
