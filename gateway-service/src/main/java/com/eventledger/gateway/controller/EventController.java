package com.eventledger.gateway.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eventledger.gateway.dto.TransactionEventRequest;
import com.eventledger.gateway.dto.TransactionEventResponse;
import com.eventledger.gateway.service.EventProcessResult;
import com.eventledger.gateway.service.EventService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
@Validated
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<TransactionEventResponse> createEvent(@Valid @RequestBody TransactionEventRequest request,
                                                                HttpServletRequest servletRequest) {
        String traceId = getOrGenerateTraceId(servletRequest);
        MDC.put("traceId", traceId);
        try {
            EventProcessResult result = eventService.processEvent(request, traceId);
            return ResponseEntity.status(result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK).body(result.getResponse());
        } finally {
            MDC.remove("traceId");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionEventResponse> getEvent(@PathVariable("id") String eventId) {
        return eventService.getEventById(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<TransactionEventResponse>> getEvents(@RequestParam(name = "account", required = true) String accountId) {
        return ResponseEntity.ok(eventService.getEventsByAccount(accountId));
    }

    private String getOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = java.util.UUID.randomUUID().toString();
        }
        log.info("Trace propagated traceId={}", traceId);
        return traceId;
    }
}
