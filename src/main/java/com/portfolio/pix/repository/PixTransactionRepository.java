package com.portfolio.pix.repository;

import com.portfolio.pix.entity.PixTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PixTransactionRepository
        extends JpaRepository<PixTransaction, java.util.UUID>, JpaSpecificationExecutor<PixTransaction> {

    Optional<PixTransaction> findByIdempotencyKey(String idempotencyKey);

    Page<PixTransaction> findAll(org.springframework.data.jpa.domain.Specification<PixTransaction> spec, Pageable pageable);
}
