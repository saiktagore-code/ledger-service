package com.eventledger.account.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.servlet.http.HttpServletRequest;

class ExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void constructorsAndApiErrorExposeValues() {
        RuntimeException cause = new RuntimeException("cause");
        AccountServiceException withCause = new AccountServiceException("type", "title", 502, cause);
        AccountServiceException withoutCause = new AccountServiceException("type", "title", 502);
        ApiError error = new ApiError("type", "title", 400, "detail", "trace");

        assertSame(cause, withCause.getCause());
        assertEquals("title", withoutCause.getMessage());
        assertEquals("type", withCause.getType());
        assertEquals("title", withCause.getTitle());
        assertEquals(502, withCause.getStatus());
        assertEquals("type", error.getType());
        assertEquals("title", error.getTitle());
        assertEquals(400, error.getStatus());
        assertEquals("detail", error.getDetail());
        assertEquals("trace", error.getTraceId());
        assertNotNull(error.getTimestamp());
        assertEquals("nope", new UnauthorizedException("nope").getMessage());
    }

    @Test
    void handleValidationReturnsFieldDetails() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "must be positive"));
        Method method = Sample.class.getDeclaredMethod("call", String.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(exception, request("trace-validation"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Validation Failed", response.getBody().getTitle());
        assertEquals("amount: must be positive", response.getBody().getDetail());
        assertEquals("trace-validation", response.getBody().getTraceId());
    }

    @Test
    void handlersReturnExpectedStatusAndTitles() {
        List<ResponseEntity<ApiError>> responses = List.of(
                handler.handleUnauthorized(new UnauthorizedException("bad key"), request("trace-401")),
                handler.handleBadRequest(new BadRequestException("bad request"), request("trace-400")),
                handler.handleMalformed(new HttpMessageNotReadableException("malformed"), request("trace-malformed")),
                handler.handleGeneric(new RuntimeException("boom"), request("trace-500"))
        );

        assertEquals(401, responses.get(0).getStatusCode().value());
        assertEquals("Unauthorized", responses.get(0).getBody().getTitle());
        assertEquals(400, responses.get(1).getStatusCode().value());
        assertEquals("Bad Request", responses.get(1).getBody().getTitle());
        assertEquals(400, responses.get(2).getStatusCode().value());
        assertEquals("Malformed Request", responses.get(2).getBody().getTitle());
        assertEquals(500, responses.get(3).getStatusCode().value());
        assertEquals("Internal Server Error", responses.get(3).getBody().getTitle());
    }

    private HttpServletRequest request(String traceId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Trace-Id")).thenReturn(traceId);
        return request;
    }

    private static class Sample {
        @SuppressWarnings("unused")
        void call(String value) {
        }
    }
}
