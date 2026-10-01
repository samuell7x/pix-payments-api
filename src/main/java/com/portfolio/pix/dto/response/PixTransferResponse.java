package com.portfolio.pix.dto.response;

import com.portfolio.pix.entity.PixTransaction;
import com.portfolio.pix.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PixTransferResponse(
        UUID transactionId,
        TransactionStatus status,
        String failureReason,
        UUID sourceAccountId,
        UUID targetAccountId,
        String targetPixKey,
        BigDecimal amount,
        String description,
        Instant createdAt,
        Instant completedAt
) {
    public static PixTransferResponse from(PixTransaction tx) {
        return new PixTransferResponse(
                tx.getId(),
                tx.getStatus(),
                tx.getFailureReason(),
                tx.getSourceAccount().getId(),
                tx.getTargetAccount().getId(),
                tx.getTargetPixKey(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getCreatedAt(),
                tx.getCompletedAt()
        );
    }
}
