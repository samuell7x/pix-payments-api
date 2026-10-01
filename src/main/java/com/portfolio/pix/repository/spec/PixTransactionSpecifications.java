package com.portfolio.pix.repository.spec;

import com.portfolio.pix.entity.PixTransaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/**
 * Specifications para o extrato: evita if-chains repetitivos no service
 * e mantem a query legivel e composicional.
 */
public final class PixTransactionSpecifications {

    private PixTransactionSpecifications() {
    }

    public static Specification<PixTransaction> involvingAccount(UUID accountId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("sourceAccount").get("id"), accountId),
                cb.equal(root.get("targetAccount").get("id"), accountId)
        );
    }

    public static Specification<PixTransaction> direction(UUID accountId, String direction) {
        if (direction == null) {
            return null;
        }
        return switch (direction.toUpperCase()) {
            case "IN" -> (root, query, cb) -> cb.equal(root.get("targetAccount").get("id"), accountId);
            case "OUT" -> (root, query, cb) -> cb.equal(root.get("sourceAccount").get("id"), accountId);
            default -> null;
        };
    }

    public static Specification<PixTransaction> createdFrom(Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<PixTransaction> createdTo(Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
