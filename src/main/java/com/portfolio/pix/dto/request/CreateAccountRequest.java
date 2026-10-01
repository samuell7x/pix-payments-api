package com.portfolio.pix.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank(message = "nome do titular e obrigatorio")
        @Size(max = 150)
        String ownerName,

        @NotBlank(message = "documento do titular e obrigatorio")
        @Pattern(regexp = "\\d{11}|\\d{14}", message = "documento deve ter 11 (CPF) ou 14 (CNPJ) digitos")
        String ownerDocument,

        @NotNull(message = "saldo inicial e obrigatorio")
        @DecimalMin(value = "0.0", message = "saldo inicial nao pode ser negativo")
        BigDecimal initialBalance
) {
}
