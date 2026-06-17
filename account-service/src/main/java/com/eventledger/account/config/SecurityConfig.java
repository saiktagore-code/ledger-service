package com.eventledger.account.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.eventledger.account.exception.UnauthorizedException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityConfig extends OncePerRequestFilter {

    private final String internalApiKey;

    public SecurityConfig(@Value("${INTERNAL_API_KEY}") String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/health") || path.startsWith("/actuator") || path.startsWith("/metrics") || path.startsWith("/swagger") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("X-Internal-Api-Key");
        if (header == null || !header.equals(internalApiKey)) {
            throw new UnauthorizedException("Invalid internal API key");
        }
        filterChain.doFilter(request, response);
    }
}
