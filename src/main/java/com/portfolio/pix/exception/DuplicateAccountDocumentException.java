package com.portfolio.pix.exception;

public class DuplicateAccountDocumentException extends RuntimeException {
    public DuplicateAccountDocumentException(String document) {
        super("Ja existe uma conta cadastrada com o documento: " + document);
    }
}
