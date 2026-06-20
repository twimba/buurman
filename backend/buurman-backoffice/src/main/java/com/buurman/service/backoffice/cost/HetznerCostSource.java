package com.buurman.service.backoffice.cost;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.domain.backoffice.CostSourceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Hetzner Cloud run-rate estimate. Hetzner has no "money" API, so we sum the monthly gross price of
 * running servers + volumes from {@code /v1/pricing}. EUR, ESTIMATED. Unavailable without a token.
 */
@Component
@Slf4j
public class HetznerCostSource implements CostSource {

  private final RestClient client;
  private final ObjectMapper objectMapper;

  public HetznerCostSource(CostProperties props, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.client = props.hetzner().token().map(HetznerCostSource::build).orElse(null);
  }

  @Override
  public CostProviderId id() {
    return CostProviderId.HETZNER;
  }

  @Override
  public ProviderReading read() {
    if (client == null) {
      return ProviderReading.unavailable(id(), "Set HETZNER_BEARER_TOKEN to enable");
    }
    try {
      JsonNode pricing = get("/v1/pricing").path("pricing");
      Map<String, Map<String, Double>> serverPrices = serverPriceTable(pricing);
      double volumePerGb =
          pricing.path("volume").path("price_per_gb_month").path("gross").asDouble(0);

      double serverCost = 0;
      for (JsonNode s : get("/v1/servers?per_page=50").path("servers")) {
        String type = s.path("server_type").path("name").asText("");
        String location = s.path("datacenter").path("location").path("name").asText("");
        serverCost +=
            serverPrices
                .getOrDefault(type, Map.of())
                .getOrDefault(location, defaultPrice(type, serverPrices));
      }

      double volumeCost = 0;
      for (JsonNode v : get("/v1/volumes?per_page=50").path("volumes")) {
        volumeCost += v.path("size").asLong(0) * volumePerGb;
      }

      List<LineItem> breakdown = new ArrayList<>();
      breakdown.add(new LineItem("Servers", Math.round(serverCost * 100)));
      breakdown.add(new LineItem("Volumes", Math.round(volumeCost * 100)));
      long total = Math.round((serverCost + volumeCost) * 100);
      return ProviderReading.of(id(), CostSourceType.ESTIMATED, "EUR", total, breakdown);
    } catch (RuntimeException e) {
      log.warn("Hetzner cost estimate failed: {}", e.getMessage());
      return ProviderReading.unavailable(id(), "Hetzner API unreachable");
    }
  }

  /** server type -> location -> monthly gross EUR. */
  private static Map<String, Map<String, Double>> serverPriceTable(JsonNode pricing) {
    Map<String, Map<String, Double>> table = new HashMap<>();
    for (JsonNode type : pricing.path("server_types")) {
      Map<String, Double> byLocation = new HashMap<>();
      for (JsonNode price : type.path("prices")) {
        byLocation.put(
            price.path("location").asText(""),
            price.path("price_monthly").path("gross").asDouble(0));
      }
      table.put(type.path("name").asText(""), byLocation);
    }
    return table;
  }

  /** Fallback when a server's exact location price is missing: any price for that type. */
  private static double defaultPrice(String type, Map<String, Map<String, Double>> table) {
    return table.getOrDefault(type, Map.of()).values().stream().findFirst().orElse(0.0);
  }

  private JsonNode get(String path) {
    String body = client.get().uri(path).retrieve().body(String.class);
    try {
      return objectMapper.readTree(body == null ? "{}" : body);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Bad Hetzner response", e);
    }
  }

  private static RestClient build(String token) {
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(6));
    return RestClient.builder()
        .baseUrl("https://api.hetzner.cloud")
        .defaultHeader("Authorization", "Bearer " + token)
        .requestFactory(factory)
        .build();
  }
}
