package com.portfolio.pix.exception;

public class SelfTransferException extends RuntimeException {
    public SelfTransferException() {
        super("Nao e permitido transferir para a propria conta");
    }
}
