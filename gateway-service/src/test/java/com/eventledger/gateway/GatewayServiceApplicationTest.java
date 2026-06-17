package com.eventledger.gateway;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class GatewayServiceApplicationTest {

    @Test
    void applicationContextStartsAndStops() {
        String previousBaseUrl = System.getProperty("ACCOUNT_SERVICE_BASE_URL");
        String previousApiKey = System.getProperty("ACCOUNT_SERVICE_API_KEY");
        System.setProperty("ACCOUNT_SERVICE_BASE_URL", "http://localhost:8081");
        System.setProperty("ACCOUNT_SERVICE_API_KEY", "test-key");
        try {
            String[] args = {"--spring.main.web-application-type=none", "--spring.main.banner-mode=off"};
            ConfigurableApplicationContext context = SpringApplication.run(GatewayServiceApplication.class, args);
            assertNotNull(context);
            context.close();
        } finally {
            if (previousBaseUrl == null) {
                System.clearProperty("ACCOUNT_SERVICE_BASE_URL");
            } else {
                System.setProperty("ACCOUNT_SERVICE_BASE_URL", previousBaseUrl);
            }
            if (previousApiKey == null) {
                System.clearProperty("ACCOUNT_SERVICE_API_KEY");
            } else {
                System.setProperty("ACCOUNT_SERVICE_API_KEY", previousApiKey);
            }
        }
    }
}
