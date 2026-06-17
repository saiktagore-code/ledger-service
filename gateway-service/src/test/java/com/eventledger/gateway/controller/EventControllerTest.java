package com.eventledger.gateway.controller;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.eventledger.gateway.dto.TransactionEventRequest;
import com.eventledger.gateway.dto.TransactionEventResponse;
import com.eventledger.gateway.service.EventProcessResult;
import com.eventledger.gateway.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class EventControllerTest {

    @Test
    void createEventReturnsCreatedWhenServiceCreates() throws Exception {
        EventService svc = Mockito.mock(EventService.class);
        TransactionEventResponse resp = new TransactionEventResponse();
        resp.setEventId("e1");
        EventProcessResult result = new EventProcessResult(resp, true);
        Mockito.when(svc.processEvent(any(TransactionEventRequest.class), any(String.class))).thenReturn(result);

        EventController controller = new EventController(svc);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        TransactionEventRequest req = new TransactionEventRequest();
        req.setEventId("e1");
        req.setAccountId("acct-1");
        req.setType("CREDIT");
        req.setAmount(1.23);
        req.setCurrency("USD");
        req.setEventTimestamp(java.time.Instant.now());

        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mvc.perform(post("/events").contentType("application/json").content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("e1"));
    }

    @Test
    void getEventReturnsOkWhenFound() throws Exception {
        EventService svc = Mockito.mock(EventService.class);
        TransactionEventResponse resp = new TransactionEventResponse();
        resp.setEventId("e1");
        Mockito.when(svc.getEventById("e1")).thenReturn(Optional.of(resp));

        EventController controller = new EventController(svc);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/events/e1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("e1"));
    }

    @Test
    void getEventsReturnsList() throws Exception {
        EventService svc = Mockito.mock(EventService.class);
        TransactionEventResponse first = new TransactionEventResponse();
        first.setEventId("e1");
        TransactionEventResponse second = new TransactionEventResponse();
        second.setEventId("e2");
        Mockito.when(svc.getEventsByAccount("a1")).thenReturn(List.of(first, second));

        EventController controller = new EventController(svc);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/events").param("account", "a1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("e1"))
                .andExpect(jsonPath("$[1].eventId").value("e2"));
    }
}
