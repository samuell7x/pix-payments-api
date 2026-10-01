package com.portfolio.pix.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PixTransferRequest(

        @NotNull(message = "sourceAccountId e obrigatorio")
        UUID sourceAccountId,

        @NotBlank(message = "targetPixKey e obrigatorio")
        String targetPixKey,

        @NotNull(message = "amount e obrigatorio")
        @DecimalMin(value = "0.01", message = "amount deve ser maior que zero")
        BigDecimal amount,

        @Size(max = 280)
        String description
) {
}
