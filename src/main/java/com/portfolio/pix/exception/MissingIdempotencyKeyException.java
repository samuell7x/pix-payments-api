package com.portfolio.pix.exception;

public class MissingIdempotencyKeyException extends RuntimeException {
    public MissingIdempotencyKeyException() {
        super("Header 'Idempotency-Key' e obrigatorio para transferencias PIX");
    }
}
