package com.portfolio.pix.exception;

public class PixKeyAlreadyExistsException extends RuntimeException {
    public PixKeyAlreadyExistsException(String keyValue) {
        super("Chave PIX ja cadastrada: " + keyValue);
    }
}
