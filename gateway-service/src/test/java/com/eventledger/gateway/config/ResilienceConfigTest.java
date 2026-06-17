package com.eventledger.gateway.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class ResilienceConfigTest {

    @Test
    void instantiateResilienceConfig() {
        ResilienceConfig cfg = new ResilienceConfig();
        assertNotNull(cfg);
    }
}
