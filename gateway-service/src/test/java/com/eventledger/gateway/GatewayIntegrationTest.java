package com.eventledger.gateway;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.eventledger.gateway.dto.TransactionEventRequest;
import com.eventledger.gateway.exception.AccountServiceException;
import com.eventledger.gateway.repository.EventRepository;
import com.eventledger.gateway.service.AccountServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@SpringBootTest(properties = {
        "ACCOUNT_SERVICE_BASE_URL=http://account-service.test",
        "ACCOUNT_SERVICE_API_KEY=test-key",
        "spring.main.banner-mode=off",
        "resilience4j.retry.instances.accountServiceRetry.maxAttempts=1",
        "resilience4j.circuitbreaker.instances.accountServiceCircuitBreaker.slidingWindowSize=2",
        "resilience4j.circuitbreaker.instances.accountServiceCircuitBreaker.minimumNumberOfCalls=2",
        "resilience4j.circuitbreaker.instances.accountServiceCircuitBreaker.waitDurationInOpenState=60s"
})
@AutoConfigureMockMvc
class GatewayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setup() {
        eventRepository.deleteAll();
        reset(restTemplate);
        circuitBreaker().reset();
    }

    @Test
    void gatewayToAccountFlowPersistsEventAndPropagatesTraceHeader() throws Exception {
        when(restTemplate.exchange(eq("http://account-service.test/accounts/acct-999/transactions"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        TransactionEventRequest request = event("evt-integration", "acct-999");

        mockMvc.perform(post("/events")
                        .contentType("application/json")
                        .header("X-Trace-Id", "trace-integration")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", is("evt-integration")))
                .andExpect(jsonPath("$.status", is("APPLIED")));

        mockMvc.perform(get("/events/evt-integration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPLIED")));

        assertEquals(1, eventRepository.count());
        verify(restTemplate).exchange(eq("http://account-service.test/accounts/acct-999/transactions"),
                eq(HttpMethod.POST), Mockito.argThat(entity -> {
                    HttpEntity<?> httpEntity = (HttpEntity<?>) entity;
                    return "trace-integration".equals(httpEntity.getHeaders().getFirst("X-Trace-Id"))
                            && "test-key".equals(httpEntity.getHeaders().getFirst("X-Internal-Api-Key"));
                }), eq(Void.class));
    }

    @Test
    void circuitBreakerOpensAndStopsCallingAccountServiceAfterRepeatedFailures() {
        when(restTemplate.exchange(eq("http://account-service.test/accounts/acct-cb/transactions"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new ResourceAccessException("down"));

        TransactionEventRequest request = event("evt-cb", "acct-cb");

        assertThrows(AccountServiceException.class, () -> accountServiceClient.postTransaction(request, "trace-cb-1"));
        assertThrows(AccountServiceException.class, () -> accountServiceClient.postTransaction(request, "trace-cb-2"));

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker().getState());

        assertThrows(AccountServiceException.class, () -> accountServiceClient.postTransaction(request, "trace-cb-3"));
        verify(restTemplate, times(2)).exchange(eq("http://account-service.test/accounts/acct-cb/transactions"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
    }

    private CircuitBreaker circuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("accountServiceCircuitBreaker");
    }

    private TransactionEventRequest event(String eventId, String accountId) {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId(eventId);
        request.setAccountId(accountId);
        request.setType("CREDIT");
        request.setAmount(25.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.parse("2026-05-15T14:02:11Z"));
        return request;
    }

}
