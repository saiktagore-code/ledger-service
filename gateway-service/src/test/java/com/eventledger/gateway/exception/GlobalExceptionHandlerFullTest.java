package com.eventledger.gateway.exception;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerFullTest {

    @Test
    void handleValidationProducesBadRequest() throws NoSuchMethodException {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-Trace-Id")).thenReturn("trace-validation");

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandler.class.getMethod("handleValidation", MethodArgumentNotValidException.class, HttpServletRequest.class),
                0);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        FieldError fieldError = new FieldError("transactionEventRequest", "amount", "must be greater than zero");
        Mockito.when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);
        var response = handler.handleValidation(ex, req);

        assertEquals(400, response.getStatusCodeValue());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("https://event-ledger/errors/validation", body.getType());
        assertEquals("Validation Failed", body.getTitle());
        assertEquals("amount: must be greater than zero", body.getDetail());
        assertEquals("trace-validation", body.getTraceId());
    }

    @Test
    void handleDuplicateEventProducesConflict() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-Trace-Id")).thenReturn("trace-duplicate");

        EventAlreadyExistsException ex = new EventAlreadyExistsException("evt-1");
        var response = handler.handleDuplicateEvent(ex, req);

        assertEquals(409, response.getStatusCodeValue());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("https://event-ledger/errors/duplicate-event", body.getType());
        assertEquals("Duplicate Event", body.getTitle());
        assertEquals("Event with id 'evt-1' already exists.", body.getDetail());
        assertEquals("trace-duplicate", body.getTraceId());
    }

    @Test
    void handleMalformedRequestProducesBadRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-Trace-Id")).thenReturn("trace-malformed");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON request");
        var response = handler.handleMalformedRequest(ex, req);

        assertEquals(400, response.getStatusCodeValue());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("https://event-ledger/errors/validation", body.getType());
        assertEquals("Malformed Request", body.getTitle());
        assertEquals("Malformed JSON request", body.getDetail());
        assertEquals("trace-malformed", body.getTraceId());
    }
}
