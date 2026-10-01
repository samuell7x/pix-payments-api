package com.portfolio.pix.dto.response;

import com.portfolio.pix.entity.PixKey;
import com.portfolio.pix.entity.PixKeyType;

/**
 * Resposta de consulta de chave (simula o DICT). Nunca expoe saldo
 * ou o documento completo do titular - apenas o necessario para o
 * pagador confirmar que esta enviando para a pessoa certa.
 */
public record PixKeyLookupResponse(
        PixKeyType keyType,
        String keyValue,
        String ownerNameMasked
) {
    public static PixKeyLookupResponse from(PixKey key) {
        return new PixKeyLookupResponse(key.getKeyType(), key.getKeyValue(), mask(key.getAccount().getOwnerName()));
    }

    private static String mask(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        StringBuilder masked = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length - 1; i++) {
            masked.append(" ").append(parts[i].charAt(0)).append(".");
        }
        masked.append(" ").append(parts[parts.length - 1]);
        return masked.toString();
    }
}
