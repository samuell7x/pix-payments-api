package com.portfolio.pix.exception;

public class MaxPixKeysExceededException extends RuntimeException {
    public MaxPixKeysExceededException(int max) {
        super("Limite de " + max + " chaves PIX por conta foi atingido");
    }
}
