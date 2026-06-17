package com.eventledger.gateway.service;

public class AccountServiceFailure {
    private final String eventId;
    private final boolean retryable;

    public AccountServiceFailure(String eventId, boolean retryable) {
        this.eventId = eventId;
        this.retryable = retryable;
    }

    public String getEventId() {
        return eventId;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
