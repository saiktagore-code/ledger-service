package com.eventledger.account.exception;

public class AccountServiceException extends RuntimeException {
    private final String type;
    private final String title;
    private final int status;

    public AccountServiceException(String type, String title, int status) {
        super(title);
        this.type = type;
        this.title = title;
        this.status = status;
    }

    public AccountServiceException(String type, String title, int status, Throwable cause) {
        super(title, cause);
        this.type = type;
        this.title = title;
        this.status = status;
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
}
