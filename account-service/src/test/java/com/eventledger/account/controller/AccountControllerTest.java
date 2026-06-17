package com.eventledger.account.controller;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.eventledger.account.domain.AccountEntity;
import com.eventledger.account.dto.AccountDetailsResponse;
import com.eventledger.account.dto.TransactionEventRequest;
import com.eventledger.account.dto.TransactionEventResponse;
import com.eventledger.account.exception.GlobalExceptionHandler;
import com.eventledger.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postTransaction_returnsCreatedWhenPathMatchesPayload() throws Exception {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-1");
        request.setAccountId("acct-1");
        request.setType("CREDIT");
        request.setAmount(25.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());

        mockMvc.perform(post("/accounts/acct-1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "trace-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(accountService).applyTransaction(eq("trace-123"), any(TransactionEventRequest.class));
    }

    @Test
    void postTransaction_returnsBadRequestWhenPathDoesNotMatchPayload() throws Exception {
        TransactionEventRequest request = new TransactionEventRequest();
        request.setEventId("evt-2");
        request.setAccountId("acct-2");
        request.setType("DEBIT");
        request.setAmount(10.0);
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.now());

        mockMvc.perform(post("/accounts/acct-1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Trace-Id", "trace-789")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Path accountId does not match request payload accountId")));

        verify(accountService, never()).applyTransaction(any(), any(TransactionEventRequest.class));
    }

    @Test
    void getAccount_returnsAccountDetails() throws Exception {
        AccountDetailsResponse response = new AccountDetailsResponse();
        response.setAccountId("acct-1");
        response.setBalance(50.0);
        response.setCurrency("USD");
        response.setTransactions(java.util.List.of(new TransactionEventResponse()));

        when(accountService.getAccountDetails("acct-1")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/accounts/acct-1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void getAccount_returnsNotFoundWhenMissing() throws Exception {
        when(accountService.getAccountDetails("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/accounts/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBalance_returnsBalanceWhenAccountExists() throws Exception {
        AccountEntity account = new AccountEntity();
        account.setAccountId("acct-1");
        account.setBalance(50.0);
        account.setCurrency("USD");
        when(accountService.getAccount("acct-1")).thenReturn(Optional.of(account));

        mockMvc.perform(get("/accounts/acct-1/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"balance\":50.0")));
    }

    @Test
    void getBalance_returnsNotFoundWhenMissing() throws Exception {
        when(accountService.getAccount("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/accounts/missing/balance"))
                .andExpect(status().isNotFound());
    }
}
