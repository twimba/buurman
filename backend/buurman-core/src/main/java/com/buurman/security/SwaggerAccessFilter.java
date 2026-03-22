package com.buurman.security;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.buurman.service.FeatureFlagService;
import com.buurman.util.FeatureFlags;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SwaggerAccessFilter extends OncePerRequestFilter {

  private final FeatureFlagService featureFlagService;

  public SwaggerAccessFilter(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return !path.startsWith("/api-docs") && !path.startsWith("/swagger-ui");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (featureFlagService.isDisabled(FeatureFlags.SWAGGER)) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    filterChain.doFilter(request, response);
  }
}
