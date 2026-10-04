package com.buurman.service.backoffice.cost;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.buurman.dto.response.backoffice.cost.FxPair;
import com.buurman.dto.response.backoffice.cost.FxPairsResponse;
import com.buurman.dto.response.backoffice.cost.FxRate;
import com.buurman.dto.response.backoffice.cost.FxRatesResponse;
import com.buurman.repository.backoffice.FxPairRepository;
import com.buurman.repository.backoffice.FxRateRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Fetches live FX rates (source currency -> EUR) and stores them, so cost normalization uses the
 * current market rate from the DB. Backed by frankfurter.app (ECB data, no API key). Called before
 * each cost snapshot.
 */
@Service
@Slf4j
public class FxRateService {

  private static final String SOURCE = "frankfurter";
  private static final int MAX_BACKFILL_YEARS = 2;

  /** Minimal shape of the frankfurter response: {"date":"2026-06-22","rates":{"EUR":0.87}}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FxApiResponse(String date, Map<String, Double> rates) {}

  /** Time-series shape: {"rates":{"2026-01-02":{"EUR":0.90}, ...}}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record FxTimeSeries(Map<String, Map<String, Double>> rates) {}

  private final CostProperties props;
  private final FxRateRepository repository;
  private final FxPairRepository pairRepository;
  private final Clock clock;
  private final RestClient client;

  public FxRateService(
      CostProperties props,
      FxRateRepository repository,
      FxPairRepository pairRepository,
      Clock clock) {
    this.props = props;
    this.repository = repository;
    this.pairRepository = pairRepository;
    this.clock = clock;
    requireHttpsUrl(props.fxApiUrl());
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    // Bound a stalled upstream so a hung response can't pin the caller indefinitely.
    factory.setReadTimeout(Duration.ofSeconds(10));
    this.client = RestClient.builder().baseUrl(props.fxApiUrl()).requestFactory(factory).build();
  }

  /** Enforce TLS for the upstream FX provider; reject a misconfigured non-https base URL. */
  private static void requireHttpsUrl(String url) {
    try {
      if (!"https".equalsIgnoreCase(java.net.URI.create(url).getScheme())) {
        throw new IllegalArgumentException(
            "backoffice.cost.fx-api-url must be an https URL: " + url);
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Invalid backoffice.cost.fx-api-url: " + url, e);
    }
  }

  /**
   * Fetch and store the latest EUR rate for every tracked pair. Resilient: a failure for one
   * currency logs and leaves its previously stored rate intact.
   */
  public void refresh() {
    for (String currency : currenciesToRefresh()) {
      fetchLatest(currency);
    }
  }

  /** Currencies needing a live rate: the DB-tracked pairs, excluding EUR (the base). */
  private java.util.Set<String> currenciesToRefresh() {
    java.util.Set<String> currencies = new java.util.TreeSet<>();
    pairRepository.list().forEach(c -> currencies.add(c.toUpperCase(Locale.ROOT)));
    currencies.remove("EUR");
    return currencies;
  }

  /** Fetch + store today's rate for a single currency. Swallows/logs failures. */
  private void fetchLatest(String cur) {
    if ("EUR".equals(cur)) {
      return;
    }
    try {
      FxApiResponse body =
          client
              .get()
              .uri(
                  uri ->
                      uri.path("/v1/latest")
                          .queryParam("base", cur)
                          .queryParam("symbols", "EUR")
                          .build())
              .retrieve()
              .body(FxApiResponse.class);
      double rate =
          body == null || body.rates() == null ? 0 : body.rates().getOrDefault("EUR", 0.0);
      if (rate <= 0) {
        log.warn("FX refresh: no EUR rate returned for {}", cur);
        return;
      }
      // Date the rate by the source's effective day (ECB skips weekends/holidays), not "now".
      String dateText = body.date() == null ? "" : body.date();
      LocalDate rateDate = dateText.isBlank() ? LocalDate.now(clock) : LocalDate.parse(dateText);
      repository.upsert(cur, rateDate, BigDecimal.valueOf(rate), SOURCE);
      log.info("FX refresh: 1 {} = {} EUR (as of {})", cur, rate, rateDate);
    } catch (Exception e) {
      log.warn("FX refresh failed for {} (keeping last stored rate): {}", cur, e.getMessage());
    }
  }

  /** Fetch live rates now (on demand) and return the refreshed latest-per-currency view. */
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FxRatesResponse refreshNow() {
    refresh();
    return currentRates();
  }

  /**
   * Backfill daily rates from {@code since} through today for every configured non-EUR currency,
   * via the source's time-series endpoint (one request per currency). Resilient per currency.
   */
  // Deliberately NOT @Transactional: each per-currency upsert is independently idempotent, and we
  // must not hold a DB connection/transaction open across the N blocking HTTP calls below.
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FxRatesResponse backfill(LocalDate since) {
    LocalDate end = LocalDate.now(clock);
    if (since == null || since.isAfter(end)) {
      return currentRates();
    }
    // Bound the range so a far-past `since` can't request an unbounded span / row count.
    LocalDate floor = end.minusYears(MAX_BACKFILL_YEARS);
    LocalDate from = since.isBefore(floor) ? floor : since;
    for (String cur : currenciesToRefresh()) {
      try {
        FxTimeSeries body =
            client
                .get()
                .uri(
                    uri ->
                        uri.path("/v1/" + from + ".." + end)
                            .queryParam("base", cur)
                            .queryParam("symbols", "EUR")
                            .build())
                .retrieve()
                .body(FxTimeSeries.class);
        if (body == null || body.rates() == null) {
          log.warn("FX backfill: no rates returned for {}", cur);
          continue;
        }
        int stored = 0;
        for (Map.Entry<String, Map<String, Double>> e : body.rates().entrySet()) {
          Double rate = e.getValue() == null ? null : e.getValue().get("EUR");
          if (rate != null && rate > 0) {
            repository.upsert(cur, LocalDate.parse(e.getKey()), BigDecimal.valueOf(rate), SOURCE);
            stored++;
          }
        }
        log.info("FX backfill: stored {} {} daily rates from {} to {}", stored, cur, from, end);
      } catch (Exception ex) {
        log.warn("FX backfill failed for {}: {}", cur, ex.getMessage());
      }
    }
    return currentRates();
  }

  /** Latest stored rate per currency, for the FX view. */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FxRatesResponse currentRates() {
    return toResponse(repository.latestPerCurrency());
  }

  /** Dated rate history (optionally for one currency), newest first, for the table + graph. */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FxRatesResponse history(String currency, Integer limit) {
    int cap = (limit == null || limit <= 0) ? 730 : Math.min(limit, 5000);
    return toResponse(
        (currency == null || currency.isBlank())
            ? repository.allRecent(cap)
            : repository.history(currency.toUpperCase(Locale.ROOT), cap));
  }

  private static FxRatesResponse toResponse(java.util.List<FxRateRepository.FxRateRow> rows) {
    return new FxRatesResponse(
        rows.stream()
            .map(r -> new FxRate(r.currency(), r.rateDate(), r.rate().doubleValue(), r.source()))
            .toList());
  }

  /** Manually set/override the EUR rate for a currency on a day (defaults to today). */
  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FxRatesResponse setManualRate(String currency, double rate, LocalDate date) {
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency is required");
    }
    if (rate <= 0) {
      throw new IllegalArgumentException("FX rate must be positive");
    }
    LocalDate on = date != null ? date : LocalDate.now(clock);
    repository.upsert(currency.toUpperCase(Locale.ROOT), on, BigDecimal.valueOf(rate), "manual");
    return currentRates();
  }

  /** Delete the stored rate for a currency on a specific day. */
  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FxRatesResponse deleteRate(String currency, LocalDate date) {
    repository.delete(currency.toUpperCase(Locale.ROOT), date);
    return currentRates();
  }

  /** All tracked pairs with their latest stored rate (rate fields empty until first fetch). */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FxPairsResponse listPairs() {
    Map<String, FxRateRepository.FxRateRow> latest =
        repository.latestPerCurrency().stream()
            .collect(
                java.util.stream.Collectors.toMap(FxRateRepository.FxRateRow::currency, r -> r));
    List<FxPair> pairs =
        pairRepository.list().stream()
            .map(
                cur -> {
                  FxRateRepository.FxRateRow r = latest.get(cur);
                  return new FxPair(
                      cur,
                      Optional.ofNullable(r).map(x -> x.rate().doubleValue()),
                      Optional.ofNullable(r).map(FxRateRepository.FxRateRow::rateDate),
                      Optional.ofNullable(r).map(FxRateRepository.FxRateRow::source));
                })
            .toList();
    return new FxPairsResponse(pairs);
  }

  /**
   * Start tracking a pair (source currency → EUR) and immediately fetch today's rate.
   *
   * <p>Deliberately NOT {@code @Transactional}: {@link #fetchLatest(String)} performs a blocking
   * outbound HTTP call, so we must not hold a DB connection/transaction open across it. The pair
   * insert and the rate upsert are each independently idempotent; a fetch failure is swallowed and
   * leaves the (already-added) pair without a stored rate until the next refresh.
   */
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FxPairsResponse addPair(String currency, String by) {
    String cur = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    if (!cur.matches("[A-Z]{3}") || "EUR".equals(cur)) {
      throw new IllegalArgumentException("Invalid currency: " + currency);
    }
    pairRepository.add(cur, by);
    fetchLatest(cur);
    return listPairs();
  }

  /** Stop tracking a pair and remove all its stored rates. */
  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public FxPairsResponse removePair(String currency) {
    String cur = currency.toUpperCase(Locale.ROOT);
    repository.deleteAll(cur);
    pairRepository.remove(cur);
    return listPairs();
  }
}
