package com.buurman.service.backoffice.cost;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.domain.backoffice.CostSourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Cloudflare account subscriptions → monthly cost. Annual subscriptions are amortized to a monthly
 * figure. SUBSCRIPTION confidence. Unavailable without a token + account id.
 */
@Component
@Slf4j
public class CloudflareCostSource implements CostSource {

  private final RestClient client;
  private final String accountId;
  private final ObjectMapper objectMapper;

  public CloudflareCostSource(CostProperties props, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.accountId = props.cloudflare().accountId().orElse(null);
    this.client =
        props.cloudflare().token().isPresent() && accountId != null
            ? build(props.cloudflare().token().orElseThrow())
            : null;
  }

  @Override
  public CostProviderId id() {
    return CostProviderId.CLOUDFLARE;
  }

  /**
   * Months covered by one billing period of the given Cloudflare subscription frequency, used to
   * amortize the period price into a monthly figure. Cloudflare's documented enum is weekly /
   * monthly / quarterly / yearly; "annual" is accepted as a yearly alias. Unknown values are
   * treated conservatively as monthly (no amortization) with a warning.
   */
  private double monthsPerPeriod(String frequency) {
    String freq = frequency == null ? "" : frequency.toLowerCase(Locale.ROOT);
    return switch (freq) {
      case "weekly" -> 1.0 / 4.33;
      case "monthly", "" -> 1.0;
      case "quarterly" -> 3.0;
      case "yearly", "annual" -> 12.0;
      default -> {
        log.warn("Unknown Cloudflare subscription frequency '{}'; treating as monthly", frequency);
        yield 1.0;
      }
    };
  }

  @Override
  public ProviderReading read() {
    if (client == null) {
      return ProviderReading.unavailable(id(), "Set Cloudflare token + account id to enable");
    }
    try {
      String body =
          client
              .get()
              .uri("/client/v4/accounts/{id}/subscriptions", accountId)
              .retrieve()
              .body(String.class);
      JsonNode result = objectMapper.readTree(body == null ? "{}" : body).path("result");
      if (!result.isArray() || result.isEmpty()) {
        return ProviderReading.of(id(), CostSourceType.SUBSCRIPTION, "USD", 0, List.of());
      }
      double monthly = 0;
      String currency = "USD";
      List<LineItem> breakdown = new ArrayList<>();
      for (JsonNode sub : result) {
        double price = sub.path("price").asDouble(0);
        String freq = sub.path("frequency").asText("monthly");
        double perMonth = price / monthsPerPeriod(freq);
        currency = sub.path("currency").asText(currency);
        monthly += perMonth;
        breakdown.add(
            new LineItem(
                sub.path("product").path("name").asText("Subscription"),
                Math.round(perMonth * 100)));
      }
      return ProviderReading.of(
          id(),
          CostSourceType.SUBSCRIPTION,
          currency.toUpperCase(Locale.ROOT),
          Math.round(monthly * 100),
          breakdown);
    } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
      log.warn("Cloudflare cost fetch failed: {}", e.getMessage());
      return ProviderReading.unavailable(id(), "Cloudflare API unreachable");
    }
  }

  private static RestClient build(String token) {
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(6));
    return RestClient.builder()
        .baseUrl("https://api.cloudflare.com")
        .defaultHeader("Authorization", "Bearer " + token)
        .requestFactory(factory)
        .build();
  }
}
