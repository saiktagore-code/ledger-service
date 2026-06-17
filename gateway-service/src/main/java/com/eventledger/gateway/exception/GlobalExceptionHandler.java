package com.eventledger.gateway.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ApiError error = new ApiError("https://event-ledger/errors/validation", "Validation Failed", 400, detail, traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccountServiceException.class)
    public ResponseEntity<ApiError> handleAccountServiceError(AccountServiceException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        ApiError error = new ApiError(ex.getType(), ex.getTitle(), ex.getStatus(), ex.getDetail(), traceId);
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(EventAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleDuplicateEvent(EventAlreadyExistsException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        ApiError error = new ApiError("https://event-ledger/errors/duplicate-event", "Duplicate Event", 409, ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedRequest(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        ApiError error = new ApiError("https://event-ledger/errors/validation", "Malformed Request", 400, ex.getMessage(), traceId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error", ex);
        String traceId = request.getHeader("X-Trace-Id");
        ApiError error = new ApiError("https://event-ledger/errors/internal", "Internal Server Error", 500, "Unexpected error", traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
