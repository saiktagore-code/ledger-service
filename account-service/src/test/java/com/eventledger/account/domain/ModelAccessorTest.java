package com.eventledger.account.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.eventledger.account.dto.TransactionEventResponse;

class ModelAccessorTest {

    @Test
    void accountAndTransactionEntitiesExposeProperties() {
        AccountEntity account = new AccountEntity();
        account.setAccountId("acct-1");
        account.setCurrency("USD");
        account.setBalance(12.5);

        assertNull(account.getId());
        assertNull(account.getVersion());
        assertEquals("acct-1", account.getAccountId());
        assertEquals("USD", account.getCurrency());
        assertEquals(12.5, account.getBalance());

        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        TransactionEntity transaction = new TransactionEntity();
        transaction.setEventId("evt-1");
        transaction.setAccountId("acct-1");
        transaction.setType("CREDIT");
        transaction.setCurrency("USD");
        transaction.setAmount(25.0);
        transaction.setEventTimestamp(timestamp);
        transaction.setCreatedAt(timestamp.plusSeconds(1));
        transaction.setMetadata(Map.of("source", "test"));

        assertNull(transaction.getId());
        assertEquals("evt-1", transaction.getEventId());
        assertEquals("acct-1", transaction.getAccountId());
        assertEquals("CREDIT", transaction.getType());
        assertEquals("USD", transaction.getCurrency());
        assertEquals(25.0, transaction.getAmount());
        assertEquals(timestamp, transaction.getEventTimestamp());
        assertEquals(timestamp.plusSeconds(1), transaction.getCreatedAt());
        assertEquals("test", transaction.getMetadata().get("source"));
    }

    @Test
    void transactionEventResponseExposesProperties() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        TransactionEventResponse response = new TransactionEventResponse();
        response.setEventId("evt-1");
        response.setAccountId("acct-1");
        response.setType("DEBIT");
        response.setAmount(10.0);
        response.setCurrency("USD");
        response.setEventTimestamp(timestamp);
        response.setMetadata(Map.of("key", "value"));

        assertEquals("evt-1", response.getEventId());
        assertEquals("acct-1", response.getAccountId());
        assertEquals("DEBIT", response.getType());
        assertEquals(10.0, response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals(timestamp, response.getEventTimestamp());
        assertEquals("value", response.getMetadata().get("key"));
    }
}
