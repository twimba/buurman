package com.buurman.security;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS = 10;
  private static final long WINDOW_MS = 60_000; // 1 minute
  private static final String RATE_LIMITED_PATH = "/registration-invitations/validate";

  private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> requestLog =
      new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!RATE_LIMITED_PATH.equals(request.getRequestURI())
        || !"POST".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = getClientIp(request);
    long now = System.currentTimeMillis();

    ConcurrentLinkedDeque<Long> timestamps =
        requestLog.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

    // Remove expired entries
    while (!timestamps.isEmpty() && timestamps.peekFirst() < now - WINDOW_MS) {
      timestamps.pollFirst();
    }

    if (timestamps.size() >= MAX_REQUESTS) {
      response.setStatus(429);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
      return;
    }

    timestamps.addLast(now);
    filterChain.doFilter(request, response);
  }

  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
