package com.eventledger.account.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class HealthControllerTest {

    @Test
    void healthReturnsStatusAndCounts() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        meterRegistry.counter("requests").increment();
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        when(accountRepository.count()).thenReturn(2L);
        when(transactionRepository.count()).thenReturn(3L);

        ResponseEntity<Map<String, Object>> response =
                new HealthController(meterRegistry, accountRepository, transactionRepository).health();

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertEquals("UP", body.get("status"));
        assertEquals("connected", body.get("database"));
        assertEquals(2L, body.get("accountCount"));
        assertEquals(3L, body.get("transactionCount"));
        assertEquals(1, body.get("meterCount"));
    }

    @Test
    void metricsReturnsMeterNamesForSimpleRegistry() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        meterRegistry.counter("account_transactions_total").increment();
        AccountRepository accountRepository = mock(AccountRepository.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);

        String metrics = new HealthController(meterRegistry, accountRepository, transactionRepository).metrics();

        assertEquals("account_transactions_total", metrics);
    }
}
