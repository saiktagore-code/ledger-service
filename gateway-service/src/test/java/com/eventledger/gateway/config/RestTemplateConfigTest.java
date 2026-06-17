package com.eventledger.gateway.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

class RestTemplateConfigTest {

    @Test
    void restTemplateBeanCreated() {
        RestTemplateConfig cfg = new RestTemplateConfig();
        RestTemplateBuilder builder = new RestTemplateBuilder();
        RestTemplate rt = cfg.restTemplate(builder);
        assertNotNull(rt);
    }
}
