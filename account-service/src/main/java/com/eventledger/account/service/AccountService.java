package com.eventledger.account.service;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventledger.account.domain.AccountEntity;
import com.eventledger.account.domain.TransactionEntity;
import com.eventledger.account.dto.AccountDetailsResponse;
import com.eventledger.account.dto.TransactionEventRequest;
import com.eventledger.account.dto.TransactionEventResponse;
import com.eventledger.account.exception.BadRequestException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final Counter accountTransactions;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository, MeterRegistry meterRegistry) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountTransactions = meterRegistry.counter("account_transactions_total", "service", "account");
    }

    @Transactional
    public synchronized void applyTransaction(String traceId, TransactionEventRequest request) {
        log.info("Applying transaction traceId={} eventId={}", traceId, request.getEventId());

        if (transactionRepository.findByEventId(request.getEventId()).isPresent()) {
            log.info("Duplicate transaction ignored traceId={} eventId={}", traceId, request.getEventId());
            return;
        }

        if (!"USD".equals(request.getCurrency())) {
            throw new BadRequestException("Unsupported currency: " + request.getCurrency());
        }

        AccountEntity account = accountRepository.findByAccountId(request.getAccountId())
                .orElseGet(() -> createAccount(request.getAccountId(), request.getCurrency()));

        double delta = "CREDIT".equals(request.getType()) ? request.getAmount() : -request.getAmount();
        account.setBalance(account.getBalance() + delta);
        accountRepository.save(account);

        TransactionEntity transaction = new TransactionEntity();
        transaction.setEventId(request.getEventId());
        transaction.setAccountId(request.getAccountId());
        transaction.setType(request.getType());
        transaction.setCurrency(request.getCurrency());
        transaction.setAmount(request.getAmount());
        transaction.setEventTimestamp(request.getEventTimestamp());
        transaction.setCreatedAt(Instant.now());
        transactionRepository.save(transaction);
        accountTransactions.increment();
    }

    private AccountEntity createAccount(String accountId, String currency) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setCurrency(currency);
        account.setBalance(0.0);
        return accountRepository.save(account);
    }

    public Optional<Double> getBalance(String accountId) {
        return accountRepository.findByAccountId(accountId).map(AccountEntity::getBalance);
    }

    public Optional<AccountEntity> getAccount(String accountId) {
        return accountRepository.findByAccountId(accountId);
    }

    public Optional<AccountDetailsResponse> getAccountDetails(String accountId) {
        return accountRepository.findByAccountId(accountId).map(account -> {
            AccountDetailsResponse response = new AccountDetailsResponse();
            response.setAccountId(account.getAccountId());
            response.setCurrency(account.getCurrency());
            response.setBalance(account.getBalance());
            response.setTransactions(transactionRepository.findByAccountIdOrderByEventTimestampAsc(accountId)
                    .stream()
                    .map(this::toResponse)
                    .toList());
            return response;
        });
    }

    private TransactionEventResponse toResponse(TransactionEntity transaction) {
        TransactionEventResponse response = new TransactionEventResponse();
        response.setEventId(transaction.getEventId());
        response.setAccountId(transaction.getAccountId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setEventTimestamp(transaction.getEventTimestamp());
        response.setMetadata(transaction.getMetadata());
        return response;
    }
}
