package com.eventledger.account.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

@RestController
public class HealthController {

    private final MeterRegistry meterRegistry;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public HealthController(MeterRegistry meterRegistry, AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.meterRegistry = meterRegistry;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "database", "connected",
                "accountCount", accountRepository.count(),
                "transactionCount", transactionRepository.count(),
                "meterCount", meterRegistry.getMeters().size()
        ));
    }

    @GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
    public String metrics() {
        if (meterRegistry instanceof PrometheusMeterRegistry prometheusMeterRegistry) {
            return prometheusMeterRegistry.scrape();
        }
        return meterRegistry.getMeters().stream()
                .map(meter -> meter.getId().getName())
                .sorted()
                .collect(Collectors.joining("\n"));
    }
}
