package com.eventledger.gateway.service;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventledger.gateway.domain.EventEntity;
import com.eventledger.gateway.domain.EventStatus;
import com.eventledger.gateway.dto.TransactionEventRequest;
import com.eventledger.gateway.exception.AccountServiceException;
import com.eventledger.gateway.repository.EventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class EventServiceDuplicateProcessingTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    private EventService eventService;

    @BeforeEach
    void setup() {
        eventService = new EventService(eventRepository, accountServiceClient, new SimpleMeterRegistry());
    }

    @Test
    void processEvent_updatesExistingFailedEventAndThrows() {
        EventEntity existing = new EventEntity();
        existing.setEventId("evt-1");
        existing.setAccountId("acct-1");
        existing.setType("CREDIT");
        existing.setAmount(10.0);
        existing.setCurrency("USD");
        existing.setEventTimestamp(Instant.now());
        existing.setStatus(EventStatus.PROCESSING);

        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-1");
        request.setAccountId("acct-1");
        request.setType("CREDIT");
        request.setAmount(10.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());

        when(eventRepository.findByEventId("evt-1")).thenReturn(Optional.of(existing));
        doThrow(new AccountServiceException("t", "title", 500, true)).when(accountServiceClient).postTransaction(any(TransactionEventRequest.class), any(String.class));

        AccountServiceException thrown = assertThrows(AccountServiceException.class, () -> eventService.processEvent(request, "trace-3"));
        assertEquals("title", thrown.getMessage());
        assertEquals(EventStatus.FAILED, existing.getStatus());
    }
}
