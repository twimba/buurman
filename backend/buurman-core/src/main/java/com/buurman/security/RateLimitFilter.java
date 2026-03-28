package com.buurman.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.buurman.domain.RateLimitConfig;
import com.buurman.service.MetricsService;
import com.buurman.service.RateLimitConfigService;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

  /** Map of "METHOD:path" → rate limit config key. Add new entries to rate-limit more endpoints. */
  private static final Map<String, String> RATE_LIMITED_ENDPOINTS =
      Map.of(
          "POST:/registration-invitations/validate", "registration-validation",
          "GET:/auth/verify-email-token", "email-token-verification",
          "POST:/auth/verify-email", "email-code-verification",
          "POST:/auth/impersonate/exchange", "impersonation-exchange");

  private final ProxyManager<String> proxyManager;
  private final RateLimitConfigService rateLimitConfigService;
  private final MetricsService metricsService;

  public RateLimitFilter(
      ProxyManager<String> rateLimitProxyManager,
      RateLimitConfigService rateLimitConfigService,
      MetricsService metricsService) {
    this.proxyManager = rateLimitProxyManager;
    this.rateLimitConfigService = rateLimitConfigService;
    this.metricsService = metricsService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String lookupKey = request.getMethod().toUpperCase(Locale.ROOT) + ":" + request.getRequestURI();
    String configKey = RATE_LIMITED_ENDPOINTS.get(lookupKey);

    if (configKey == null) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = getClientIp(request);

    try {
      RateLimitConfig config = rateLimitConfigService.getConfig(configKey).orElse(null);

      if (config == null || !config.isEnabled()) {
        filterChain.doFilter(request, response);
        return;
      }

      String bucketKey = configKey + ":" + clientIp;
      BucketProxy bucket = proxyManager.getProxy(bucketKey, () -> buildBucketConfiguration(config));
      ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

      if (probe.isConsumed()) {
        metricsService.incrementCounter("ratelimit.allowed.total", "endpoint", configKey);
        metricsService.recordHistogram(
            "ratelimit.remaining_tokens", probe.getRemainingTokens(), "endpoint", configKey);
        filterChain.doFilter(request, response);
        return;
      }

      long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;

      log.warn(
          "Rate limit exceeded for IP {} on endpoint {} [{}] — retry after {}s",
          clientIp,
          request.getRequestURI(),
          configKey,
          retryAfterSeconds);
      metricsService.incrementCounter("ratelimit.rejected.total", "endpoint", configKey);
      metricsService.recordHistogram(
          "ratelimit.remaining_tokens", probe.getRemainingTokens(), "endpoint", configKey);

      response.setStatus(429);
      response.setContentType("application/json");
      response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
      response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
    } catch (Exception e) {
      // Fail-open: if the database is unreachable, allow the request through
      log.error("Rate limit check failed for IP {} — allowing request through", clientIp, e);
      metricsService.incrementCounter("ratelimit.error.total", "endpoint", configKey);
      filterChain.doFilter(request, response);
    }
  }

  private BucketConfiguration buildBucketConfiguration(RateLimitConfig config) {
    return BucketConfiguration.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(config.getMaxRequests())
                .refillIntervally(
                    config.getMaxRequests(), Duration.ofSeconds(config.getPeriodSeconds()))
                .build())
        .build();
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",", 2)[0].trim();
    }
    return request.getRemoteAddr();
  }
}
