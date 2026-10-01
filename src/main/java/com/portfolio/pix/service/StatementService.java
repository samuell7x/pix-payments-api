package com.portfolio.pix.service;

import com.portfolio.pix.entity.PixTransaction;
import com.portfolio.pix.repository.PixTransactionRepository;
import com.portfolio.pix.repository.spec.PixTransactionSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final PixTransactionRepository transactionRepository;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public Page<PixTransaction> getStatement(UUID accountId, String direction, Instant from, Instant to, Pageable pageable) {
        accountService.findById(accountId); // 404 se a conta nao existir

        Specification<PixTransaction> spec = Specification
                .where(PixTransactionSpecifications.involvingAccount(accountId))
                .and(PixTransactionSpecifications.direction(accountId, direction))
                .and(PixTransactionSpecifications.createdFrom(from))
                .and(PixTransactionSpecifications.createdTo(to));

        return transactionRepository.findAll(spec, pageable);
    }
}
