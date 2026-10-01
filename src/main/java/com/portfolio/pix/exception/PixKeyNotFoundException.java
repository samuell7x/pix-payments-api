package com.portfolio.pix.exception;

public class PixKeyNotFoundException extends RuntimeException {
    public PixKeyNotFoundException(String keyValue) {
        super("Chave PIX nao encontrada: " + keyValue);
    }
}
