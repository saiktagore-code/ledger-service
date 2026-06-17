package com.eventledger.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.eventledger.gateway.dto.TransactionEventRequest;
import com.eventledger.gateway.exception.AccountServiceException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AccountServiceClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private SimpleMeterRegistry meterRegistry;
    private AccountServiceClient client;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        meterRegistry = new SimpleMeterRegistry();
        client = new AccountServiceClient(restTemplate, "http://localhost:8081", "secret-key", meterRegistry);
    }

    @Test
    void postTransaction_incrementsMeterOnSuccess() {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-1");
        request.setAccountId("acct-1");
        request.setType("CREDIT");
        request.setAmount(50.0);
        request.setCurrency("USD");
        request.setEventTimestamp(java.time.Instant.now());

        server.expect(MockRestRequestMatchers.requestTo("http://localhost:8081/accounts/acct-1/transactions"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.header("X-Trace-Id", "trace-1"))
                .andExpect(MockRestRequestMatchers.header("X-Internal-Api-Key", "secret-key"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CREATED));

        client.postTransaction(request, "trace-1");
        server.verify();

        Counter successCounter = meterRegistry.get("account_service_calls_total").counter();
        Counter failureCounter = meterRegistry.get("account_service_failures_total").counter();

        assertEquals(1.0, successCounter.count());
        assertEquals(0.0, failureCounter.count());
    }

    @Test
    void postTransaction_recordsFailureForServerError() {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-2");
        request.setAccountId("acct-2");
        request.setType("DEBIT");
        request.setAmount(75.0);
        request.setCurrency("USD");
        request.setEventTimestamp(java.time.Instant.now());

        server.expect(MockRestRequestMatchers.requestTo("http://localhost:8081/accounts/acct-2/transactions"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        AccountServiceException exception = assertThrows(AccountServiceException.class,
                () -> client.postTransaction(request, "trace-2"));

        // AccountServiceClient translates server 5xx to AccountServiceException with original status
        assertEquals(500, exception.getStatus());

        Counter failureCounter = meterRegistry.get("account_service_failures_total").counter();
        assertEquals(1.0, failureCounter.count());
    }

    @Test
    void postTransaction_translatesClientErrorAsNonRetryableException() {
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        SimpleMeterRegistry localRegistry = new SimpleMeterRegistry();
        AccountServiceClient localClient = new AccountServiceClient(mockRestTemplate, "http://localhost:8081", "secret-key", localRegistry);

        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-3");
        request.setAccountId("acct-3");
        request.setType("DEBIT");
        request.setAmount(20.0);
        request.setCurrency("USD");
        request.setEventTimestamp(java.time.Instant.now());

        Mockito.when(mockRestTemplate.exchange(Mockito.eq("http://localhost:8081/accounts/acct-3/transactions"),
                Mockito.eq(HttpMethod.POST), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", new HttpHeaders(), null, null));

        AccountServiceException exception = assertThrows(AccountServiceException.class,
                () -> localClient.postTransaction(request, "trace-3"));

        assertEquals(400, exception.getStatus());
        assertEquals(false, exception.isRetryable());
        assertEquals(1.0, localRegistry.get("account_service_failures_total").counter().count());
    }

    @Test
    void postTransaction_translatesResourceAccessExceptionAsRetryableException() {
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        SimpleMeterRegistry localRegistry = new SimpleMeterRegistry();
        AccountServiceClient localClient = new AccountServiceClient(mockRestTemplate, "http://localhost:8081", "secret-key", localRegistry);

        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-4");
        request.setAccountId("acct-4");
        request.setType("CREDIT");
        request.setAmount(40.0);
        request.setCurrency("USD");
        request.setEventTimestamp(java.time.Instant.now());

        Mockito.when(mockRestTemplate.exchange(Mockito.eq("http://localhost:8081/accounts/acct-4/transactions"),
                Mockito.eq(HttpMethod.POST), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        AccountServiceException exception = assertThrows(AccountServiceException.class,
                () -> localClient.postTransaction(request, "trace-4"));

        assertEquals(503, exception.getStatus());
        assertEquals(true, exception.isRetryable());
        assertEquals(1.0, localRegistry.get("account_service_failures_total").counter().count());
    }

    @Test
    void fallback_throwsAccountServiceException() {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-5");
        request.setAccountId("acct-5");
        request.setType("DEBIT");
        request.setAmount(60.0);
        request.setCurrency("USD");
        request.setEventTimestamp(java.time.Instant.now());

        RuntimeException cause = new RuntimeException("down");
        AccountServiceException exception = assertThrows(AccountServiceException.class,
                () -> client.fallback(request, "trace-5", cause));

        assertEquals(503, exception.getStatus());
        assertEquals(true, exception.isRetryable());
        assertEquals("ACCOUNT_SERVICE_UNAVAILABLE", exception.getTitle());
        assertEquals("down", exception.getDetail());
        assertEquals(cause, exception.getCause());
    }
}
