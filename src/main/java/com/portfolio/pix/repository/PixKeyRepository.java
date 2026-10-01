package com.portfolio.pix.repository;

import com.portfolio.pix.entity.PixKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixKeyRepository extends JpaRepository<PixKey, UUID> {

    Optional<PixKey> findByKeyValue(String keyValue);

    boolean existsByKeyValue(String keyValue);

    long countByAccountId(UUID accountId);

    List<PixKey> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
