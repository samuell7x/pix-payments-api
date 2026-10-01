package com.portfolio.pix.dto.response;

import com.portfolio.pix.entity.PixKey;
import com.portfolio.pix.entity.PixKeyType;

import java.time.Instant;
import java.util.UUID;

public record PixKeyResponse(
        UUID id,
        PixKeyType keyType,
        String keyValue,
        UUID accountId,
        Instant createdAt
) {
    public static PixKeyResponse from(PixKey key) {
        return new PixKeyResponse(
                key.getId(),
                key.getKeyType(),
                key.getKeyValue(),
                key.getAccount().getId(),
                key.getCreatedAt()
        );
    }
}
