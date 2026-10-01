package com.portfolio.pix.controller;

import com.portfolio.pix.dto.request.CreateAccountRequest;
import com.portfolio.pix.dto.response.AccountResponse;
import com.portfolio.pix.entity.Account;
import com.portfolio.pix.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * Endpoint auxiliar para criar contas de teste. Nao existe autenticacao
 * neste projeto de portfolio (ver README) - em um cenario real, a conta
 * seria criada por um fluxo de onboarding com KYC, fora do escopo aqui.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(request);
        AccountResponse body = AccountResponse.from(account);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + account.getId())).body(body);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> findById(@PathVariable UUID accountId) {
        Account account = accountService.findById(accountId);
        return ResponseEntity.ok(AccountResponse.from(account));
    }
}
