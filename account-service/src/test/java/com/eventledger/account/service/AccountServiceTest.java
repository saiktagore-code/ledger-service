package com.eventledger.account.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventledger.account.domain.AccountEntity;
import com.eventledger.account.domain.TransactionEntity;
import com.eventledger.account.dto.TransactionEventRequest;
import com.eventledger.account.exception.BadRequestException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private AccountService accountService;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setup() {
        meterRegistry = new SimpleMeterRegistry();
        accountService = new AccountService(accountRepository, transactionRepository, meterRegistry);
        lenient().when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(transactionRepository.findByEventId(any())).thenReturn(Optional.empty());
        lenient().when(accountRepository.findByAccountId(any())).thenReturn(Optional.empty());
    }

    @Test
    void applyTransaction_createsAccountAndStoresTransaction() {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-1");
        request.setAccountId("acct-1");
        request.setType("CREDIT");
        request.setAmount(150.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());

        when(transactionRepository.findByEventId("evt-1")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountId("acct-1")).thenReturn(Optional.empty());

        accountService.applyTransaction("trace-123", request);

        verify(accountRepository, times(2)).save(any(AccountEntity.class));
        verify(transactionRepository).save(any(TransactionEntity.class));
        assertEquals(1.0, meterRegistry.get("account_transactions_total").counter().count());
    }

    @Test
    void applyTransaction_ignoresDuplicateTransaction() {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-dup");
        request.setAccountId("acct-1");
        request.setType("CREDIT");
        request.setAmount(100.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());

        when(transactionRepository.findByEventId("evt-dup")).thenReturn(Optional.of(new TransactionEntity()));

        accountService.applyTransaction("trace-dup", request);

        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void applyTransaction_rejectsUnsupportedCurrency() {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-2");
        request.setAccountId("acct-1");
        request.setType("DEBIT");
        request.setAmount(20.0);
        request.setCurrency("EUR");
        request.setEventTimestamp(Instant.now());

        when(transactionRepository.findByEventId("evt-2")).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> accountService.applyTransaction("trace-456", request));

        assertTrue(exception.getMessage().contains("Unsupported currency"));
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void applyTransaction_debitsExistingAccount() {
        AccountEntity account = new AccountEntity();
        account.setAccountId("acct-1");
        account.setCurrency("USD");
        account.setBalance(100.0);

        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-debit");
        request.setAccountId("acct-1");
        request.setType("DEBIT");
        request.setAmount(40.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());

        when(accountRepository.findByAccountId("acct-1")).thenReturn(Optional.of(account));

        accountService.applyTransaction("trace-debit", request);

        assertEquals(60.0, account.getBalance());
        verify(accountRepository).save(account);
        verify(transactionRepository).save(any(TransactionEntity.class));
    }

    @Test
    void getBalanceAndAccountDelegateToRepository() {
        AccountEntity account = new AccountEntity();
        account.setAccountId("acct-1");
        account.setCurrency("USD");
        account.setBalance(75.0);
        when(accountRepository.findByAccountId("acct-1")).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountId("missing")).thenReturn(Optional.empty());

        assertEquals(Optional.of(75.0), accountService.getBalance("acct-1"));
        assertEquals(Optional.of(account), accountService.getAccount("acct-1"));
        assertEquals(Optional.empty(), accountService.getBalance("missing"));
    }

    @Test
    void getAccountDetailsMapsTransactionsInTimestampOrder() {
        AccountEntity account = new AccountEntity();
        account.setAccountId("acct-1");
        account.setCurrency("USD");
        account.setBalance(125.0);

        TransactionEntity transaction = new TransactionEntity();
        transaction.setEventId("evt-1");
        transaction.setAccountId("acct-1");
        transaction.setType("CREDIT");
        transaction.setAmount(125.0);
        transaction.setCurrency("USD");
        transaction.setEventTimestamp(Instant.parse("2026-01-01T00:00:00Z"));
        transaction.setMetadata(Map.of("source", "unit-test"));

        when(accountRepository.findByAccountId("acct-1")).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountIdOrderByEventTimestampAsc("acct-1")).thenReturn(List.of(transaction));

        var response = accountService.getAccountDetails("acct-1").orElseThrow();

        assertEquals("acct-1", response.getAccountId());
        assertEquals("USD", response.getCurrency());
        assertEquals(125.0, response.getBalance());
        assertEquals("evt-1", response.getTransactions().get(0).getEventId());
        assertEquals("unit-test", response.getTransactions().get(0).getMetadata().get("source"));
    }
}
