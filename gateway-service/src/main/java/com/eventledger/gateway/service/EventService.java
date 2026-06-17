package com.eventledger.gateway.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventledger.gateway.domain.EventEntity;
import com.eventledger.gateway.domain.EventStatus;
import com.eventledger.gateway.dto.TransactionEventRequest;
import com.eventledger.gateway.dto.TransactionEventResponse;
import com.eventledger.gateway.repository.EventRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final EventRepository eventRepository;
    private final AccountServiceClient accountServiceClient;
    private final Counter eventSubmissions;
    private final Counter eventDuplicates;
    private final Counter eventFailures;

    public EventService(EventRepository eventRepository,
                        AccountServiceClient accountServiceClient,
                        MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.accountServiceClient = accountServiceClient;
        this.eventSubmissions = meterRegistry.counter("events_received_total", "service", "gateway");
        this.eventDuplicates = meterRegistry.counter("events_duplicate_total", "service", "gateway");
        this.eventFailures = meterRegistry.counter("events_failed_total", "service", "gateway");
    }

    @Transactional
    public synchronized EventProcessResult processEvent(TransactionEventRequest request, String traceId) {
        log.info("Processing event with traceId={}, eventId={}", traceId, request.getEventId());
        eventSubmissions.increment();

        Optional<EventEntity> existing = eventRepository.findByEventId(request.getEventId());
        if (existing.isPresent()) {
            EventEntity existingEvent = existing.get();
            if (existingEvent.getStatus() == EventStatus.APPLIED) {
                eventDuplicates.increment();
                log.info("Duplicate event detected traceId={}, eventId={}", traceId, request.getEventId());
                return new EventProcessResult(toResponse(existingEvent), false);
            }
            existingEvent.setStatus(EventStatus.PROCESSING);
            eventRepository.save(existingEvent);
            return processExistingEvent(existingEvent, request, traceId);
        }

        EventEntity eventEntity = buildEventEntity(request);
        eventRepository.save(eventEntity);
        eventEntity.setStatus(EventStatus.PROCESSING);
        eventRepository.save(eventEntity);

        return applyEvent(eventEntity, request, traceId, true);
    }

    private EventProcessResult processExistingEvent(EventEntity eventEntity, TransactionEventRequest request, String traceId) {
        EventProcessResult result = applyEvent(eventEntity, request, traceId, false);
        return result;
    }

    private EventProcessResult applyEvent(EventEntity eventEntity, TransactionEventRequest request, String traceId, boolean created) {
        try {
            accountServiceClient.postTransaction(request, traceId);
            eventEntity.setStatus(EventStatus.APPLIED);
            eventRepository.save(eventEntity);
            return new EventProcessResult(toResponse(eventEntity), created);
        } catch (Exception ex) {
            log.error("Failed to apply event traceId={} eventId={} error={}", traceId, request.getEventId(), ex.getMessage());
            eventEntity.setStatus(EventStatus.FAILED);
            eventRepository.save(eventEntity);
            eventFailures.increment();
            throw ex;
        }
    }

    private EventEntity buildEventEntity(TransactionEventRequest request) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(request.getEventId());
        eventEntity.setAccountId(request.getAccountId());
        eventEntity.setType(request.getType());
        eventEntity.setAmount(request.getAmount());
        eventEntity.setCurrency(request.getCurrency());
        eventEntity.setEventTimestamp(request.getEventTimestamp());
        eventEntity.setMetadata(request.getMetadata());
        eventEntity.setCreatedAt(Instant.now());
        eventEntity.setStatus(EventStatus.RECEIVED);
        return eventEntity;
    }

    public Optional<TransactionEventResponse> getEventById(String eventId) {
        return eventRepository.findByEventId(eventId).map(this::toResponse);
    }

    public List<TransactionEventResponse> getEventsByAccount(String accountId) {
        return eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TransactionEventResponse toResponse(EventEntity entity) {
        TransactionEventResponse response = new TransactionEventResponse();
        response.setEventId(entity.getEventId());
        response.setAccountId(entity.getAccountId());
        response.setType(entity.getType());
        response.setAmount(entity.getAmount());
        response.setCurrency(entity.getCurrency());
        response.setEventTimestamp(entity.getEventTimestamp());
        response.setStatus(entity.getStatus());
        response.setMetadata(entity.getMetadata());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
