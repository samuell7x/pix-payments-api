package com.portfolio.pix.repository;

import com.portfolio.pix.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Lock pessimista de escrita. Deve SEMPRE ser chamado com os IDs
     * ordenados (ver PixTransferService) para evitar deadlock quando
     * duas transferencias concorrentes envolvem as mesmas duas contas
     * em ordem invertida.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(UUID id);

    boolean existsByOwnerDocument(String ownerDocument);
}
