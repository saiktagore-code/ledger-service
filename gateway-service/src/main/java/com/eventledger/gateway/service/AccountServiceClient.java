package com.eventledger.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.eventledger.gateway.dto.TransactionEventRequest;
import com.eventledger.gateway.exception.AccountServiceException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);
    private final RestTemplate restTemplate;
    private final String accountServiceBaseUrl;
    private final String apiKey;
    private final Counter accountServiceCalls;
    private final Counter accountServiceFailures;

    public AccountServiceClient(RestTemplate restTemplate,
                                @Value("${ACCOUNT_SERVICE_BASE_URL}") String accountServiceBaseUrl,
                                @Value("${ACCOUNT_SERVICE_API_KEY}") String apiKey,
                                MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.accountServiceBaseUrl = accountServiceBaseUrl;
        this.apiKey = apiKey;
        this.accountServiceCalls = meterRegistry.counter("account_service_calls_total", "service", "gateway");
        this.accountServiceFailures = meterRegistry.counter("account_service_failures_total", "service", "gateway");
    }

    @Retry(name = "accountServiceRetry")
    @CircuitBreaker(name = "accountServiceCircuitBreaker", fallbackMethod = "fallback")
    public void postTransaction(TransactionEventRequest request, String traceId) {
        String url = String.format("%s/accounts/%s/transactions", accountServiceBaseUrl, request.getAccountId());
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", apiKey);
        headers.set("X-Trace-Id", traceId);
        HttpEntity<TransactionEventRequest> entity = new HttpEntity<>(request, headers);

        try {
            accountServiceCalls.increment();
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                accountServiceFailures.increment();
                throw new AccountServiceException("https://event-ledger/errors/account-service", "Account Service returned non-success status", response.getStatusCodeValue(), false);
            }
        } catch (HttpStatusCodeException ex) {
            accountServiceFailures.increment();
            boolean retryable = ex.getStatusCode().is5xxServerError();
            String type = retryable ? "https://event-ledger/errors/account-service" : "https://event-ledger/errors/account-service-client";
            throw new AccountServiceException(type, ex.getStatusText(), ex.getRawStatusCode(), retryable, ex);
        } catch (ResourceAccessException ex) {
            accountServiceFailures.increment();
            throw new AccountServiceException("https://event-ledger/errors/account-service", "Unable to reach Account Service", 503, true, ex);
        }
    }

    public void fallback(TransactionEventRequest request, String traceId, Throwable throwable) {
        log.error("Account service fallback activated traceId={} eventId={} cause={}", traceId, request.getEventId(), throwable.getMessage());
        throw new AccountServiceException("https://event-ledger/errors/account-service", "ACCOUNT_SERVICE_UNAVAILABLE", 503, true, throwable);
    }
}
