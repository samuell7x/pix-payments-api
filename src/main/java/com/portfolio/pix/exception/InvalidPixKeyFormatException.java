package com.portfolio.pix.exception;

import com.portfolio.pix.entity.PixKeyType;

public class InvalidPixKeyFormatException extends RuntimeException {
    public InvalidPixKeyFormatException(PixKeyType type, String value) {
        super("Valor '" + value + "' nao e valido para o tipo de chave " + type);
    }
}
