package com.eventledger.gateway.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventledger.gateway.domain.EventEntity;
import com.eventledger.gateway.domain.EventStatus;
import com.eventledger.gateway.dto.TransactionEventRequest;
import com.eventledger.gateway.dto.TransactionEventResponse;
import com.eventledger.gateway.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    private EventService eventService;
    private io.micrometer.core.instrument.simple.SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setup() {
        Mockito.lenient().when(eventRepository.save(any(EventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        eventService = new EventService(eventRepository, accountServiceClient, meterRegistry);
    }

    @Test
    void processEvent_appliesNewEventAndMarksAsApplied() {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-1");
        request.setAccountId("acct-1");
        request.setType("CREDIT");
        request.setAmount(100.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());

        when(eventRepository.findByEventId("evt-1")).thenReturn(Optional.empty());

        EventProcessResult result = eventService.processEvent(request, "trace-1");

        assertTrue(result.isCreated());
        TransactionEventResponse response = result.getResponse();
        assertEquals("evt-1", response.getEventId());
        verify(accountServiceClient).postTransaction(request, "trace-1");
    }

    @Test
    void processEvent_returnsExistingEventWhenDuplicate() {
        EventEntity existingEvent = new EventEntity();
        existingEvent.setEventId("evt-1");
        existingEvent.setAccountId("acct-1");
        existingEvent.setAmount(100.0);
        existingEvent.setCurrency("USD");
        existingEvent.setType("CREDIT");
        existingEvent.setEventTimestamp(Instant.now());
        existingEvent.setStatus(EventStatus.APPLIED);

        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-1");
        request.setAccountId("acct-1");
        request.setType("CREDIT");
        request.setAmount(100.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());

        when(eventRepository.findByEventId("evt-1")).thenReturn(Optional.of(existingEvent));

        EventProcessResult result = eventService.processEvent(request, "trace-2");

        assertFalse(result.isCreated());
        assertEquals("evt-1", result.getResponse().getEventId());
        verify(accountServiceClient, never()).postTransaction(any(TransactionEventRequest.class), any());
    }

    @Test
    void getEventsByAccount_returnsEventsInOrder() {
        EventEntity first = new EventEntity();
        first.setEventId("evt-1");
        first.setAccountId("acct-1");
        first.setEventTimestamp(Instant.parse("2025-01-01T00:00:00Z"));
        first.setStatus(EventStatus.APPLIED);

        EventEntity second = new EventEntity();
        second.setEventId("evt-2");
        second.setAccountId("acct-1");
        second.setEventTimestamp(Instant.parse("2025-01-01T01:00:00Z"));
        second.setStatus(EventStatus.APPLIED);

        when(eventRepository.findByAccountIdOrderByEventTimestampAsc("acct-1"))
                .thenReturn(List.of(first, second));

        var events = eventService.getEventsByAccount("acct-1");

        assertEquals(2, events.size());
        assertEquals("evt-1", events.get(0).getEventId());
        assertEquals("evt-2", events.get(1).getEventId());
    }

    @Test
    void processEvent_updatesExistingProcessingEvent() {
        EventEntity existingEvent = new EventEntity();
        existingEvent.setEventId("evt-2");
        existingEvent.setAccountId("acct-2");
        existingEvent.setType("DEBIT");
        existingEvent.setAmount(50.0);
        existingEvent.setCurrency("USD");
        existingEvent.setEventTimestamp(Instant.now());
        existingEvent.setStatus(EventStatus.PROCESSING);

        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-2");
        request.setAccountId("acct-2");
        request.setType("DEBIT");
        request.setAmount(50.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());

        when(eventRepository.findByEventId("evt-2")).thenReturn(Optional.of(existingEvent));

        EventProcessResult result = eventService.processEvent(request, "trace-4");

        assertFalse(result.isCreated());
        assertEquals("evt-2", result.getResponse().getEventId());
        assertEquals(EventStatus.APPLIED, existingEvent.getStatus());
        Mockito.verify(accountServiceClient).postTransaction(request, "trace-4");
    }

    @Test
    void getEventById_returnsEmptyWhenNotFound() {
        when(eventRepository.findByEventId("evt-99")).thenReturn(Optional.empty());

        var response = eventService.getEventById("evt-99");

        assertFalse(response.isPresent());
    }
}
