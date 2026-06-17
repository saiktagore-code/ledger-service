package com.eventledger.account.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestSizeFilter extends OncePerRequestFilter {

    private static final long MAX_REQUEST_SIZE = 1024 * 1024; // 1 MB

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String contentLength = request.getHeader("Content-Length");
        if (contentLength != null) {
            try {
                long length = Long.parseLong(contentLength);
                if (length > MAX_REQUEST_SIZE) {
                    response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"PAYLOAD_TOO_LARGE\",\"message\":\"Request body exceeds maximum allowed size\"}");
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }
}
