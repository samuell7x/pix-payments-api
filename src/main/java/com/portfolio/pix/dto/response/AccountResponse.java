package com.portfolio.pix.dto.response;

import com.portfolio.pix.entity.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String ownerName,
        String ownerDocument,
        BigDecimal balance,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getOwnerName(),
                account.getOwnerDocument(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
