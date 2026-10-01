package com.portfolio.pix.dto.request;

import com.portfolio.pix.entity.PixKeyType;
import jakarta.validation.constraints.NotNull;

public record CreatePixKeyRequest(

        @NotNull(message = "accountId e obrigatorio")
        java.util.UUID accountId,

        @NotNull(message = "keyType e obrigatorio")
        PixKeyType keyType,

        /*
         * Opcional apenas para keyType = EVP (chave aleatoria).
         * Para os demais tipos e obrigatorio e sera validado no service
         * de acordo com o formato esperado do tipo.
         */
        String keyValue
) {
}
