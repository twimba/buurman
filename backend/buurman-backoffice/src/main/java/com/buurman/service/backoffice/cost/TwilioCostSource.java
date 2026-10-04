package com.buurman.service.backoffice.cost;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.buurman.config.models.TwilioProperties;
import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.domain.backoffice.CostSourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Twilio actual month-to-date spend via the Usage Records API ({@code totalprice} category), in the
 * account's billing currency. ACTUAL. Unavailable without credentials.
 */
@Component
@Slf4j
public class TwilioCostSource implements CostSource {

  private final String accountSid;
  private final RestClient client;
  private final ObjectMapper objectMapper;

  public TwilioCostSource(TwilioProperties props, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.accountSid = props.accountSid();
    this.client =
        isBlank(props.accountSid()) || isBlank(props.authToken())
            ? null
            : build(props.accountSid(), props.authToken());
  }

  @Override
  public CostProviderId id() {
    return CostProviderId.TWILIO;
  }

  @Override
  public ProviderReading read() {
    if (client == null) {
      return ProviderReading.unavailable(id(), "Set Twilio credentials to enable");
    }
    try {
      String body =
          client
              .get()
              .uri(
                  "/2010-04-01/Accounts/{sid}/Usage/Records/ThisMonth.json?Category=totalprice",
                  accountSid)
              .retrieve()
              .body(String.class);
      JsonNode records = objectMapper.readTree(body == null ? "{}" : body).path("usage_records");
      if (!records.isArray() || records.isEmpty()) {
        return ProviderReading.of(id(), CostSourceType.ACTUAL, "EUR", 0, List.of());
      }
      JsonNode record = records.get(0);
      double price = record.path("price").asDouble(0);
      String currency = record.path("price_unit").asText("USD").toUpperCase(Locale.ROOT);
      return ProviderReading.of(
          id(), CostSourceType.ACTUAL, currency, Math.round(Math.abs(price) * 100), List.of());
    } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
      log.warn("Twilio cost fetch failed: {}", e.getMessage());
      return ProviderReading.unavailable(id(), "Twilio API unreachable");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static RestClient build(String sid, String token) {
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(6));
    String basic =
        Base64.getEncoder().encodeToString((sid + ":" + token).getBytes(StandardCharsets.UTF_8));
    return RestClient.builder()
        .baseUrl("https://api.twilio.com")
        .defaultHeader("Authorization", "Basic " + basic)
        .requestFactory(factory)
        .build();
  }
}
