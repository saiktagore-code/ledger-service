package com.eventledger.account.exception;

import java.time.Instant;

public class ApiError {
    private String type;
    private String title;
    private int status;
    private String detail;
    private String traceId;
    private Instant timestamp = Instant.now();

    public ApiError(String type, String title, int status, String detail, String traceId) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.traceId = traceId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
