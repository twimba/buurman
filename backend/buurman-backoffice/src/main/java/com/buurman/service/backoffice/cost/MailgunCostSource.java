package com.buurman.service.backoffice.cost;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.buurman.config.models.MailgunProperties;
import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.domain.backoffice.CostSourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Mailgun volume-based estimate: configured flat plan fee + accepted-email volume × per-email rate.
 * ESTIMATED (Mailgun has no money API). Unavailable without credentials.
 */
@Component
@Slf4j
public class MailgunCostSource implements CostSource {

  private final RestClient client;
  private final String domain;
  private final CostConfigService config;
  private final ObjectMapper objectMapper;

  public MailgunCostSource(
      MailgunProperties mailgun, CostConfigService config, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.domain = mailgun.domain();
    this.config = config;
    boolean configured =
        mailgun.apiKey() != null
            && !mailgun.apiKey().isBlank()
            && domain != null
            && !domain.isBlank();
    this.client = configured ? build(mailgun.apiKey(), mailgun.euRegion()) : null;
  }

  @Override
  public CostProviderId id() {
    return CostProviderId.MAILGUN;
  }

  @Override
  public ProviderReading read() {
    if (client == null) {
      return ProviderReading.unavailable(id(), "Set Mailgun credentials to enable");
    }
    try {
      String body =
          client
              .get()
              .uri("/v3/{domain}/stats/total?event=accepted&duration=1m&resolution=month", domain)
              .retrieve()
              .body(String.class);
      long accepted = 0;
      for (JsonNode stat : objectMapper.readTree(body == null ? "{}" : body).path("stats")) {
        accepted += stat.path("accepted").path("total").asLong(0);
      }
      // Read the editable parameters at snapshot time so admin edits take effect without a restart.
      double baseEur = config.mailgunBaseEur();
      double volumeCost = accepted * config.mailgunPerEmailEur();
      List<LineItem> breakdown =
          List.of(
              new LineItem("Plan", Math.round(baseEur * 100)),
              new LineItem(accepted + " emails", Math.round(volumeCost * 100)));
      long total = Math.round((baseEur + volumeCost) * 100);
      return ProviderReading.of(id(), CostSourceType.ESTIMATED, "EUR", total, breakdown);
    } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
      log.warn("Mailgun cost estimate failed: {}", e.getMessage());
      return ProviderReading.unavailable(id(), "Mailgun API unreachable");
    }
  }

  private static RestClient build(String apiKey, boolean euRegion) {
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(6));
    String basic =
        Base64.getEncoder().encodeToString(("api:" + apiKey).getBytes(StandardCharsets.UTF_8));
    String baseUrl = euRegion ? "https://api.eu.mailgun.net" : "https://api.mailgun.net";
    return RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Basic " + basic)
        .requestFactory(factory)
        .build();
  }
}
