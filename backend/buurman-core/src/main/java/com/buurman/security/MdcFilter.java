package com.buurman.security;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MdcFilter extends OncePerRequestFilter {

  private static final String REQUEST_ID = "requestId";
  private static final String USER_ID = "userId";
  private static final String TEAM_ID = "teamId";
  private static final String USER_EMAIL = "userEmail";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      MDC.put(REQUEST_ID, UUID.randomUUID().toString());

      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null
          && authentication.getPrincipal() instanceof UserPrincipal principal) {
        MDC.put(USER_ID, principal.getUserId().toString());
        principal.getTeamId().ifPresent(id -> MDC.put(TEAM_ID, id.toString()));
        MDC.put(USER_EMAIL, principal.getEmail());
      }

      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
