package com.eventledger.gateway.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    @Test
    void handleAccountServiceErrorProducesApiError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Trace-Id")).thenReturn("trace-1");

        AccountServiceException ex = new AccountServiceException("t", "title", 502, true);
        var resp = handler.handleAccountServiceError(ex, req);

        assertEquals(502, resp.getStatusCodeValue());
        ApiError body = resp.getBody();
        assertNotNull(body);
        assertEquals("t", body.getType());
        assertEquals("title", body.getTitle());
        assertEquals("trace-1", body.getTraceId());
    }

    @Test
    void handleGenericProducesInternalError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Trace-Id")).thenReturn(null);

        Exception ex = new RuntimeException("boom");
        var resp = handler.handleGeneric(ex, req);

        assertEquals(500, resp.getStatusCodeValue());
        ApiError body = resp.getBody();
        assertNotNull(body);
        assertEquals("Internal Server Error", body.getTitle());
    }
}
