package com.eventledger.account.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventledger.account.dto.AccountDetailsResponse;
import com.eventledger.account.dto.TransactionEventRequest;
import com.eventledger.account.exception.BadRequestException;
import com.eventledger.account.service.AccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
@Validated
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<Void> postTransaction(@PathVariable("accountId") String accountId,
                                                @Valid @RequestBody TransactionEventRequest request,
                                                HttpServletRequest requestContext) {
        String traceId = requestContext.getHeader("X-Trace-Id");
        MDC.put("traceId", traceId);
        try {
            if (!accountId.equals(request.getAccountId())) {
                throw new BadRequestException("Path accountId does not match request payload accountId");
            }
            log.info("Received transaction request traceId={} eventId={}", traceId, request.getEventId());
            accountService.applyTransaction(traceId, request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } finally {
            MDC.remove("traceId");
        }
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable("accountId") String accountId) {
        return accountService.getAccount(accountId)
                .map(account -> ResponseEntity.<Map<String, Object>>ok(Map.of("accountId", accountId, "balance", account.getBalance(), "currency", account.getCurrency())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDetailsResponse> getAccount(@PathVariable("accountId") String accountId) {
        return accountService.getAccountDetails(accountId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
