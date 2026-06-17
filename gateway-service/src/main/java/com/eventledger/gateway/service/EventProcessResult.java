package com.eventledger.gateway.service;

import com.eventledger.gateway.dto.TransactionEventResponse;

public class EventProcessResult {
    private final TransactionEventResponse response;
    private final boolean created;

    public EventProcessResult(TransactionEventResponse response, boolean created) {
        this.response = response;
        this.created = created;
    }

    public TransactionEventResponse getResponse() {
        return response;
    }

    public boolean isCreated() {
        return created;
    }
}
