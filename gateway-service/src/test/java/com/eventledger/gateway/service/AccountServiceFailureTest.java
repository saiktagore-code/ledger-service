package com.eventledger.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class AccountServiceFailureTest {

    @Test
    void gettersReturnConstructedValues() {
        AccountServiceFailure failure = new AccountServiceFailure("evt-1", true);

        assertEquals("evt-1", failure.getEventId());
        assertEquals(true, failure.isRetryable());
    }
}
