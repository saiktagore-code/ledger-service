package com.eventledger.gateway.dto;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class TransactionEventRequest {

    @NotBlank
    private String eventId;

    @NotBlank
    @Pattern(regexp = "acct-[A-Za-z0-9_-]+")
    private String accountId;

    @NotBlank
    @Pattern(regexp = "CREDIT|DEBIT")
    private String type;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private Double amount;

    @NotBlank
    @Pattern(regexp = "USD")
    private String currency;

    @NotNull
    private Instant eventTimestamp;

    private Map<String, String> metadata;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
