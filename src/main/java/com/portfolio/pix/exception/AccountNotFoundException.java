package com.portfolio.pix.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID accountId) {
        super("Conta nao encontrada: " + accountId);
    }
}
