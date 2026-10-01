package com.portfolio.pix.service;

import com.portfolio.pix.dto.request.PixTransferRequest;
import com.portfolio.pix.entity.Account;
import com.portfolio.pix.entity.PixKey;
import com.portfolio.pix.entity.PixTransaction;
import com.portfolio.pix.entity.TransactionStatus;
import com.portfolio.pix.exception.AccountNotFoundException;
import com.portfolio.pix.exception.InvalidTransferAmountException;
import com.portfolio.pix.exception.PixKeyNotFoundException;
import com.portfolio.pix.exception.SelfTransferException;
import com.portfolio.pix.repository.AccountRepository;
import com.portfolio.pix.repository.PixKeyRepository;
import com.portfolio.pix.repository.PixTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Servico responsavel pela transferencia PIX. Os dois problemas de
 * concorrencia enderecados aqui:
 *
 * 1) LOST UPDATE entre debito e credito: resolvido com lock pessimista
 *    (SELECT ... FOR UPDATE) nas duas contas, adquirido SEMPRE na
 *    mesma ordem (menor UUID primeiro) para eliminar a possibilidade
 *    de deadlock entre duas transferencias concorrentes e opostas
 *    (A->B ao mesmo tempo que B->A).
 *
 * 2) DUPLICACAO por retry de rede: resolvido com Idempotency-Key unica
 *    no banco. Se duas requisicoes com a mesma chave chegarem em
 *    paralelo, a constraint UNIQUE garante que so uma seja persistida;
 *    a outra recebe DataIntegrityViolationException e devolvemos o
 *    resultado ja processado.
 */
@Service
@RequiredArgsConstructor
public class PixTransferService {

    private static final Logger log = LoggerFactory.getLogger(PixTransferService.class);

    private final AccountRepository accountRepository;
    private final PixKeyRepository pixKeyRepository;
    private final PixTransactionRepository transactionRepository;

    @Transactional
    public PixTransaction transfer(PixTransferRequest request, String idempotencyKey) {

        Optional<PixTransaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Replay idempotente detectado. idempotencyKey={} transactionId={}",
                    idempotencyKey, existing.get().getId());
            return existing.get();
        }

        validateAmount(request.amount());

        PixKey targetKey = pixKeyRepository.findByKeyValue(request.targetPixKey())
                .orElseThrow(() -> new PixKeyNotFoundException(request.targetPixKey()));

        UUID sourceAccountId = request.sourceAccountId();
        UUID targetAccountId = targetKey.getAccount().getId();

        if (sourceAccountId.equals(targetAccountId)) {
            throw new SelfTransferException();
        }

        boolean sourceFirst = sourceAccountId.compareTo(targetAccountId) < 0;
        UUID firstLockId = sourceFirst ? sourceAccountId : targetAccountId;
        UUID secondLockId = sourceFirst ? targetAccountId : sourceAccountId;

        Account first = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new AccountNotFoundException(firstLockId));
        Account second = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new AccountNotFoundException(secondLockId));

        Account source = sourceFirst ? first : second;
        Account target = sourceFirst ? second : first;

        PixTransaction transaction = PixTransaction.builder()
                .idempotencyKey(idempotencyKey)
                .sourceAccount(source)
                .targetAccount(target)
                .targetPixKey(request.targetPixKey())
                .amount(request.amount())
                .description(request.description())
                .build();

        if (source.getBalance().compareTo(request.amount()) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("INSUFFICIENT_BALANCE");
            transaction.setCompletedAt(Instant.now());
            log.info("Transferencia PIX recusada por saldo insuficiente. source={} amount={}",
                    source.getId(), request.amount());
            return persistIdempotently(transaction);
        }

        source.setBalance(source.getBalance().subtract(request.amount()));
        target.setBalance(target.getBalance().add(request.amount()));

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(Instant.now());

        PixTransaction saved = persistIdempotently(transaction);
        log.info("Transferencia PIX concluida. transactionId={} source={} target={} amount={}",
                saved.getId(), source.getId(), target.getId(), request.amount());
        return saved;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferAmountException(amount);
        }
    }

    private PixTransaction persistIdempotently(PixTransaction transaction) {
        try {
            return transactionRepository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException e) {
            // Outra requisicao com a mesma Idempotency-Key venceu a corrida.
            return transactionRepository.findByIdempotencyKey(transaction.getIdempotencyKey())
                    .orElseThrow(() -> e);
        }
    }
}
