package com.buurman.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.buurman.service.FeatureFlagService;
import com.buurman.util.FeatureFlags;

import jakarta.servlet.FilterChain;

@DisplayName("SwaggerAccessFilter")
@ExtendWith(MockitoExtension.class)
class SwaggerAccessFilterTest {

  @Mock private FeatureFlagService featureFlagService;
  @Mock private FilterChain filterChain;

  private SwaggerAccessFilter filter;

  @BeforeEach
  void setUp() {
    filter = new SwaggerAccessFilter(featureFlagService);
  }

  @Nested
  @DisplayName("shouldNotFilter")
  class ShouldNotFilter {

    @Test
    @DisplayName("filters /api-docs paths")
    void filtersApiDocs() {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api-docs/v1");
      request.setServletPath("/api-docs/v1");

      assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("filters /swagger-ui paths")
    void filtersSwaggerUi() {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
      request.setServletPath("/swagger-ui/index.html");

      assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("does not filter regular API paths")
    void doesNotFilterRegularPaths() {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/properties");
      request.setServletPath("/api/v1/properties");

      assertThat(filter.shouldNotFilter(request)).isTrue();
    }
  }

  @Nested
  @DisplayName("swagger enabled")
  class SwaggerEnabled {

    @Test
    @DisplayName("passes through when swagger feature flag is enabled")
    void passesThroughWhenEnabled() throws Exception {
      when(featureFlagService.isDisabled(FeatureFlags.SWAGGER)).thenReturn(false);

      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      verify(filterChain).doFilter(request, response);
    }
  }

  @Nested
  @DisplayName("swagger disabled")
  class SwaggerDisabled {

    @Test
    @DisplayName("returns 404 when swagger feature flag is disabled")
    void returns404WhenDisabled() throws Exception {
      when(featureFlagService.isDisabled(FeatureFlags.SWAGGER)).thenReturn(true);

      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilterInternal(request, response, filterChain);

      assertThat(response.getStatus()).isEqualTo(404);
      verifyNoMoreInteractions(filterChain);
    }
  }
}
