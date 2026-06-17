package com.eventledger.account.dto;

import java.util.List;

public class AccountDetailsResponse {
    private String accountId;
    private String currency;
    private Double balance;
    private List<TransactionEventResponse> transactions;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public List<TransactionEventResponse> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionEventResponse> transactions) {
        this.transactions = transactions;
    }
}
