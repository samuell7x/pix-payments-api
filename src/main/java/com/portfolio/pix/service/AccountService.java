package com.portfolio.pix.service;

import com.portfolio.pix.dto.request.CreateAccountRequest;
import com.portfolio.pix.entity.Account;
import com.portfolio.pix.exception.AccountNotFoundException;
import com.portfolio.pix.exception.DuplicateAccountDocumentException;
import com.portfolio.pix.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account create(CreateAccountRequest request) {
        if (accountRepository.existsByOwnerDocument(request.ownerDocument())) {
            throw new DuplicateAccountDocumentException(request.ownerDocument());
        }
        Account account = Account.builder()
                .ownerName(request.ownerName())
                .ownerDocument(request.ownerDocument())
                .balance(request.initialBalance())
                .build();
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account findById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
