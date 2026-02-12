package com.buurman.security;

import com.buurman.config.DemoDataProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DemoApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Demo-Api-Key";
    private static final String DEMO_DATA_PATH = "/admin/demo-data";

    private final DemoDataProperties properties;

    public DemoApiKeyFilter(DemoDataProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(DEMO_DATA_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || !apiKey.equals(properties.apiKey())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
