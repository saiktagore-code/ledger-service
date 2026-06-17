package com.eventledger.gateway.exception;

public class EventAlreadyExistsException extends RuntimeException {
    public EventAlreadyExistsException(String eventId) {
        super("Event with id '" + eventId + "' already exists.");
    }
}
