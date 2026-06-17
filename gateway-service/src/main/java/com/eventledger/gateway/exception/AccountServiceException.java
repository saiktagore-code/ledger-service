package com.eventledger.gateway.exception;

public class AccountServiceException extends RuntimeException {
    private final String type;
    private final String title;
    private final int status;
    private final boolean retryable;
    private final String detail;

    public AccountServiceException(String type, String title, int status, boolean retryable) {
        super(title);
        this.type = type;
        this.title = title;
        this.status = status;
        this.retryable = retryable;
        this.detail = title;
    }

    public AccountServiceException(String type, String title, int status, boolean retryable, Throwable cause) {
        super(title, cause);
        this.type = type;
        this.title = title;
        this.status = status;
        this.retryable = retryable;
        this.detail = cause.getMessage();
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

    public boolean isRetryable() {
        return retryable;
    }

    public String getDetail() {
        return detail;
    }
}
