package com.eventledger.gateway.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventledger.gateway.repository.EventRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

@RestController
public class HealthController {

    private final MeterRegistry meterRegistry;
    private final EventRepository eventRepository;

    public HealthController(MeterRegistry meterRegistry, EventRepository eventRepository) {
        this.meterRegistry = meterRegistry;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "database", "connected",
                "eventCount", eventRepository.count(),
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
