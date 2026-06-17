package com.eventledger.gateway.controller;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eventledger.gateway.repository.EventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class HealthControllerTest {

    @Test
    void healthReturnsStatusAndCounts() {
        EventRepository repo = mock(EventRepository.class);
        when(repo.count()).thenReturn(7L);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HealthController controller = new HealthController(registry, repo);

        var response = controller.health();
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertEquals("UP", body.get("status"));
        assertEquals("connected", body.get("database"));
        assertEquals(7L, body.get("eventCount"));
        assertEquals(0, body.get("meterCount"));
    }

    @Test
    void metricsReturnsMeterNamesForSimpleRegistry() {
        EventRepository repo = mock(EventRepository.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.counter("events_received_total").increment();
        HealthController controller = new HealthController(registry, repo);

        assertEquals("events_received_total", controller.metrics());
    }
}
