package com.eventledger.gateway.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class ApiErrorTest {

    @Test
    void gettersReturnAssignedValues() {
        ApiError a = new ApiError("t", "title", 400, "detail", "trace");
        assertEquals("t", a.getType());
        assertEquals("title", a.getTitle());
        assertEquals(400, a.getStatus());
        assertEquals("detail", a.getDetail());
        assertEquals("trace", a.getTraceId());
        assertNotNull(a.getTimestamp());
    }
}
