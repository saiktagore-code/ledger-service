package com.eventledger.gateway.domain;

public enum EventStatus {
    RECEIVED,
    PROCESSING,
    APPLIED,
    FAILED,
    DUPLICATE,
    PENDING_RETRY
}
