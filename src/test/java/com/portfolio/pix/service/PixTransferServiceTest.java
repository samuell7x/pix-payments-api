package com.portfolio.pix.service;

import com.portfolio.pix.dto.request.PixTransferRequest;
import com.portfolio.pix.entity.*;
import com.portfolio.pix.exception.AccountNotFoundException;
import com.portfolio.pix.exception.InvalidTransferAmountException;
import com.portfolio.pix.exception.PixKeyNotFoundException;
import com.portfolio.pix.exception.SelfTransferException;
import com.portfolio.pix.repository.AccountRepository;
import com.portfolio.pix.repository.PixKeyRepository;
import com.portfolio.pix.repository.PixTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PixTransferServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PixKeyRepository pixKeyRepository;
    @Mock
    private PixTransactionRepository transactionRepository;

    @InjectMocks
    private PixTransferService pixTransferService;

    private Account source;
    private Account target;
    private PixKey targetKey;

    @BeforeEach
    void setUp() {
        source = Account.builder().id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .ownerName("Alice").ownerDocument("11111111111").balance(new BigDecimal("100.00")).build();
        target = Account.builder().id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .ownerName("Bob").ownerDocument("22222222222").balance(new BigDecimal("50.00")).build();
        targetKey = PixKey.builder().id(UUID.randomUUID()).keyType(PixKeyType.EMAIL)
                .keyValue("bob@example.com").account(target).build();
    }

    @Test
    void deveTransferirComSucessoQuandoSaldoSuficiente() {
        when(transactionRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(pixKeyRepository.findByKeyValue("bob@example.com")).thenReturn(Optional.of(targetKey));
        when(accountRepository.findByIdForUpdate(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
        when(transactionRepository.saveAndFlush(any(PixTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PixTransferRequest request = new PixTransferRequest(source.getId(), "bob@example.com",
                new BigDecimal("30.00"), "aluguel");

        PixTransaction result = pixTransferService.transfer(request, "idem-1");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(source.getBalance()).isEqualByComparingTo("70.00");
        assertThat(target.getBalance()).isEqualByComparingTo("80.00");
    }

    @Test
    void deveMarcarComoFailedQuandoSaldoInsuficiente() {
        when(transactionRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(pixKeyRepository.findByKeyValue("bob@example.com")).thenReturn(Optional.of(targetKey));
        when(accountRepository.findByIdForUpdate(source.getId())).thenReturn(Optional.of(source));
        when(accountRepository.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
        when(transactionRepository.saveAndFlush(any(PixTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PixTransferRequest request = new PixTransferRequest(source.getId(), "bob@example.com",
                new BigDecimal("999.00"), null);

        PixTransaction result = pixTransferService.transfer(request, "idem-3");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("INSUFFICIENT_BALANCE");
        // saldo nao pode ter sido alterado numa transferencia recusada
        assertThat(source.getBalance()).isEqualByComparingTo("100.00");
        assertThat(target.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void deveRetornarTransacaoExistenteEmReplayIdempotente() {
        PixTransaction existing = PixTransaction.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("idem-replay")
                .status(TransactionStatus.COMPLETED)
                .build();
        when(transactionRepository.findByIdempotencyKey("idem-replay")).thenReturn(Optional.of(existing));

        PixTransferRequest request = new PixTransferRequest(source.getId(), "bob@example.com",
                new BigDecimal("10.00"), null);

        PixTransaction result = pixTransferService.transfer(request, "idem-replay");

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(pixKeyRepository, accountRepository);
    }

    @Test
    void deveLancarExcecaoQuandoChaveDestinoNaoExiste() {
        when(transactionRepository.findByIdempotencyKey("idem-4")).thenReturn(Optional.empty());
        when(pixKeyRepository.findByKeyValue("naoexiste@example.com")).thenReturn(Optional.empty());

        PixTransferRequest request = new PixTransferRequest(source.getId(), "naoexiste@example.com",
                new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> pixTransferService.transfer(request, "idem-4"))
                .isInstanceOf(PixKeyNotFoundException.class);
    }

    @Test
    void deveLancarExcecaoParaAutoTransferencia() {
        PixKey ownKey = PixKey.builder().id(UUID.randomUUID()).keyType(PixKeyType.EMAIL)
                .keyValue("alice@example.com").account(source).build();
        when(transactionRepository.findByIdempotencyKey("idem-5")).thenReturn(Optional.empty());
        when(pixKeyRepository.findByKeyValue("alice@example.com")).thenReturn(Optional.of(ownKey));

        PixTransferRequest request = new PixTransferRequest(source.getId(), "alice@example.com",
                new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> pixTransferService.transfer(request, "idem-5"))
                .isInstanceOf(SelfTransferException.class);
    }

    @Test
    void deveLancarExcecaoParaValorInvalido() {
        when(transactionRepository.findByIdempotencyKey("idem-6")).thenReturn(Optional.empty());

        PixTransferRequest request = new PixTransferRequest(source.getId(), "bob@example.com",
                BigDecimal.ZERO, null);

        assertThatThrownBy(() -> pixTransferService.transfer(request, "idem-6"))
                .isInstanceOf(InvalidTransferAmountException.class);
    }
}
