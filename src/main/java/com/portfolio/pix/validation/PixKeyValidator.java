package com.portfolio.pix.validation;

import com.portfolio.pix.entity.PixKeyType;
import com.portfolio.pix.exception.InvalidPixKeyFormatException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validacao de FORMATO das chaves PIX. Simplificacao assumida: para
 * CPF/CNPJ validamos apenas quantidade e padrao de digitos, sem o
 * calculo do digito verificador (mod 11) do documento real - fora do
 * escopo deste projeto de portfolio, mas facil de plugar aqui depois.
 */
@Component
public class PixKeyValidator {

    private static final Pattern CPF = Pattern.compile("\\d{11}");
    private static final Pattern CNPJ = Pattern.compile("\\d{14}");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+55\\d{10,11}$");

    public void validate(PixKeyType type, String value) {
        boolean valid = switch (type) {
            case CPF -> value != null && CPF.matcher(value).matches();
            case CNPJ -> value != null && CNPJ.matcher(value).matches();
            case EMAIL -> value != null && EMAIL.matcher(value).matches();
            case PHONE -> value != null && PHONE.matcher(value).matches();
            case EVP -> true; // gerada pelo sistema, nao vem do usuario
        };

        if (!valid) {
            throw new InvalidPixKeyFormatException(type, value);
        }
    }
}
