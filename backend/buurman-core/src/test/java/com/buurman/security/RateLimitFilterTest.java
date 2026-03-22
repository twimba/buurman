package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.buurman.domain.RateLimitConfig;
import com.buurman.service.MetricsService;
import com.buurman.service.RateLimitConfigService;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;

@DisplayName("RateLimitFilter")
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

  @Mock private ProxyManager<String> proxyManager;
  @Mock private RateLimitConfigService rateLimitConfigService;
  @Mock private MetricsService metricsService;
  @Mock private FilterChain filterChain;
  @Mock private BucketProxy bucketProxy;
  @Mock private ConsumptionProbe consumptionProbe;

  private RateLimitFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RateLimitFilter(proxyManager, rateLimitConfigService, metricsService);
  }

  @Nested
  @DisplayName("non-rate-limited endpoints")
  class NonRateLimited {

    @Test
    @DisplayName("passes through for non-rate-limited endpoint")
    void passesThroughNonRateLimited() throws Exception {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
      request.setRequestURI("/api/v1/properties");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
      verify(rateLimitConfigService, never()).getConfig(anyString());
    }
  }

  @Nested
  @DisplayName("rate-limited endpoints mapping")
  class EndpointMapping {

    @ParameterizedTest(name = "{0} {1} -> config key {2}")
    @CsvSource({
      "POST,/registration-invitations/validate,registration-validation",
      "GET,/auth/verify-email-token,email-token-verification",
      "POST,/auth/verify-email,email-code-verification",
      "POST,/auth/impersonate/exchange,impersonation-exchange"
    })
    @DisplayName("maps rate-limited endpoint to config key")
    void mapsEndpointToConfigKey(String method, String path, String configKey) throws Exception {
      when(rateLimitConfigService.getConfig(configKey)).thenReturn(Optional.empty());

      MockHttpServletRequest request = new MockHttpServletRequest(method, path);
      request.setRequestURI(path);
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(rateLimitConfigService).getConfig(configKey);
      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("disabled or missing config")
  class DisabledConfig {

    @Test
    @DisplayName("passes through when no config found for endpoint")
    void passesThroughWhenNoConfig() throws Exception {
      when(rateLimitConfigService.getConfig("registration-validation"))
          .thenReturn(Optional.empty());

      MockHttpServletRequest request =
          new MockHttpServletRequest("POST", "/registration-invitations/validate");
      request.setRequestURI("/registration-invitations/validate");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("passes through when config is disabled")
    void passesThroughWhenDisabled() throws Exception {
      RateLimitConfig config = RateLimitConfig.builder().enabled(false).build();
      when(rateLimitConfigService.getConfig("registration-validation"))
          .thenReturn(Optional.of(config));

      MockHttpServletRequest request =
          new MockHttpServletRequest("POST", "/registration-invitations/validate");
      request.setRequestURI("/registration-invitations/validate");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("rate limiting behavior")
  class RateLimitingBehavior {

    @Test
    @DisplayName("allows request when bucket has tokens")
    @SuppressWarnings("unchecked")
    void allowsWhenTokenAvailable() throws Exception {
      RateLimitConfig config =
          RateLimitConfig.builder().enabled(true).maxRequests(10).periodSeconds(60).build();
      when(rateLimitConfigService.getConfig("registration-validation"))
          .thenReturn(Optional.of(config));
      when(proxyManager.getProxy(anyString(), any(Supplier.class))).thenReturn(bucketProxy);
      when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
      when(consumptionProbe.isConsumed()).thenReturn(true);

      MockHttpServletRequest request =
          new MockHttpServletRequest("POST", "/registration-invitations/validate");
      request.setRequestURI("/registration-invitations/validate");
      request.setRemoteAddr("192.168.1.1");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("returns 429 with Retry-After header when rate limited")
    @SuppressWarnings("unchecked")
    void returns429WhenRateLimited() throws Exception {
      RateLimitConfig config =
          RateLimitConfig.builder().enabled(true).maxRequests(5).periodSeconds(60).build();
      when(rateLimitConfigService.getConfig("registration-validation"))
          .thenReturn(Optional.of(config));
      when(proxyManager.getProxy(anyString(), any(Supplier.class))).thenReturn(bucketProxy);
      when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
      when(consumptionProbe.isConsumed()).thenReturn(false);
      when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L); // 30 seconds

      MockHttpServletRequest request =
          new MockHttpServletRequest("POST", "/registration-invitations/validate");
      request.setRequestURI("/registration-invitations/validate");
      request.setRemoteAddr("10.0.0.1");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      assertThat(response.getStatus()).isEqualTo(429);
      assertThat(response.getHeader("Retry-After")).isEqualTo("31"); // 30s + 1
      assertThat(response.getContentAsString()).contains("Too many requests");
      verify(filterChain, never()).doFilter(any(), any());
      verify(metricsService)
          .incrementCounter("ratelimit.rejected.total", "endpoint", "registration-validation");
    }
  }

  @Nested
  @DisplayName("client IP resolution")
  class ClientIpResolution {

    @Test
    @DisplayName("uses X-Forwarded-For header when present")
    @SuppressWarnings("unchecked")
    void usesXForwardedFor() throws Exception {
      RateLimitConfig config =
          RateLimitConfig.builder().enabled(true).maxRequests(10).periodSeconds(60).build();
      when(rateLimitConfigService.getConfig("registration-validation"))
          .thenReturn(Optional.of(config));
      when(proxyManager.getProxy(eq("registration-validation:203.0.113.1"), any(Supplier.class)))
          .thenReturn(bucketProxy);
      when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
      when(consumptionProbe.isConsumed()).thenReturn(true);

      MockHttpServletRequest request =
          new MockHttpServletRequest("POST", "/registration-invitations/validate");
      request.setRequestURI("/registration-invitations/validate");
      request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(proxyManager).getProxy(eq("registration-validation:203.0.113.1"), any(Supplier.class));
    }

    @Test
    @DisplayName("falls back to remoteAddr when X-Forwarded-For is absent")
    @SuppressWarnings("unchecked")
    void fallsBackToRemoteAddr() throws Exception {
      RateLimitConfig config =
          RateLimitConfig.builder().enabled(true).maxRequests(10).periodSeconds(60).build();
      when(rateLimitConfigService.getConfig("registration-validation"))
          .thenReturn(Optional.of(config));
      when(proxyManager.getProxy(eq("registration-validation:127.0.0.1"), any(Supplier.class)))
          .thenReturn(bucketProxy);
      when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
      when(consumptionProbe.isConsumed()).thenReturn(true);

      MockHttpServletRequest request =
          new MockHttpServletRequest("POST", "/registration-invitations/validate");
      request.setRequestURI("/registration-invitations/validate");
      request.setRemoteAddr("127.0.0.1");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(proxyManager).getProxy(eq("registration-validation:127.0.0.1"), any(Supplier.class));
    }
  }

  @Nested
  @DisplayName("fail-open behavior")
  class FailOpen {

    @Test
    @DisplayName("allows request through when DB is unreachable (fail-open)")
    void allowsRequestOnException() throws Exception {
      RateLimitConfig config =
          RateLimitConfig.builder().enabled(true).maxRequests(10).periodSeconds(60).build();
      when(rateLimitConfigService.getConfig("registration-validation"))
          .thenReturn(Optional.of(config));
      when(proxyManager.getProxy(anyString(), any())).thenThrow(new RuntimeException("DB down"));

      MockHttpServletRequest request =
          new MockHttpServletRequest("POST", "/registration-invitations/validate");
      request.setRequestURI("/registration-invitations/validate");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
      verify(metricsService)
          .incrementCounter("ratelimit.error.total", "endpoint", "registration-validation");
    }
  }
}
