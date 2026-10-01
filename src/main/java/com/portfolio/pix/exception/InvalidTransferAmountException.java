package com.portfolio.pix.exception;

import java.math.BigDecimal;

public class InvalidTransferAmountException extends RuntimeException {
    public InvalidTransferAmountException(BigDecimal amount) {
        super("Valor de transferencia invalido: " + amount);
    }
}
