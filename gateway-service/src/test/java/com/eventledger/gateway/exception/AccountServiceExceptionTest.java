package com.eventledger.gateway.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class AccountServiceExceptionTest {

    @Test
    void simpleExceptionFields() {
        AccountServiceException ex = new AccountServiceException("t", "title", 503, false);
        assertEquals("t", ex.getType());
        assertEquals("title", ex.getTitle());
        assertEquals(503, ex.getStatus());
        assertFalse(ex.isRetryable());
        assertNotNull(ex.getDetail());
    }
}
