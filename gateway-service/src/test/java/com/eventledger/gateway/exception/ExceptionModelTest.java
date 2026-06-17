package com.eventledger.gateway.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ExceptionModelTest {

    @Test
    void eventAlreadyExistsExceptionMessage() {
        EventAlreadyExistsException ex = new EventAlreadyExistsException("evt-1");
        assertEquals("Event with id 'evt-1' already exists.", ex.getMessage());
    }

    @Test
    void badRequestExceptionMessage() {
        BadRequestException ex = new BadRequestException("bad request");
        assertEquals("bad request", ex.getMessage());
    }
}
