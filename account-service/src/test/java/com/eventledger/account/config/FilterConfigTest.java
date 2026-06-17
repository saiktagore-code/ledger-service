package com.eventledger.account.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.eventledger.account.exception.UnauthorizedException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class FilterConfigTest {

    @Test
    void requestSizeFilterRejectsOversizedRequests() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Content-Length", String.valueOf(1024 * 1024 + 1));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestSizeFilter().doFilter(request, response, new MockFilterChain());

        assertEquals(413, response.getStatus());
        assertEquals("application/json", response.getContentType());
    }

    @Test
    void requestSizeFilterAllowsMissingAndInvalidLengths() throws ServletException, IOException {
        RequestSizeFilter filter = new RequestSizeFilter();
        MockHttpServletResponse missingResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), missingResponse, new MockFilterChain());
        assertEquals(200, missingResponse.getStatus());

        MockHttpServletRequest invalidRequest = new MockHttpServletRequest();
        invalidRequest.addHeader("Content-Length", "not-a-number");
        MockHttpServletResponse invalidResponse = new MockHttpServletResponse();
        filter.doFilter(invalidRequest, invalidResponse, new MockFilterChain());
        assertEquals(200, invalidResponse.getStatus());
    }

    @Test
    void securityConfigSkipsPublicPaths() throws ServletException, IOException {
        SecurityConfig filter = new SecurityConfig("secret");
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void securityConfigRejectsMissingOrInvalidKey() {
        SecurityConfig filter = new SecurityConfig("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/accounts/acct-1/transactions");

        assertThrows(UnauthorizedException.class,
                () -> filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain()));
    }

    @Test
    void securityConfigAllowsValidKey() throws ServletException, IOException {
        SecurityConfig filter = new SecurityConfig("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/accounts/acct-1/transactions");
        request.addHeader("X-Internal-Api-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }
}
