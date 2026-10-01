package com.portfolio.pix.service;

import com.portfolio.pix.dto.request.CreatePixKeyRequest;
import com.portfolio.pix.entity.Account;
import com.portfolio.pix.entity.PixKeyType;
import com.portfolio.pix.exception.MaxPixKeysExceededException;
import com.portfolio.pix.exception.PixKeyAlreadyExistsException;
import com.portfolio.pix.repository.PixKeyRepository;
import com.portfolio.pix.validation.PixKeyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PixKeyServiceTest {

    @Mock
    private PixKeyRepository pixKeyRepository;
    @Mock
    private AccountService accountService;
    @InjectMocks
    private PixKeyService pixKeyService;

    private final PixKeyValidator realValidator = new PixKeyValidator();

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder().id(UUID.randomUUID()).ownerName("Alice")
                .ownerDocument("11111111111").balance(BigDecimal.ZERO).build();
        // injeta o validador real (nao mockado) via reflection nao e necessario
        // aqui pois o campo e final com @RequiredArgsConstructor; usamos
        // um service construido manualmente para este teste especifico.
        pixKeyService = new PixKeyService(pixKeyRepository, accountService, realValidator);
    }

    @Test
    void deveLancarExcecaoQuandoChaveJaExiste() {
        when(accountService.findById(account.getId())).thenReturn(account);
        when(pixKeyRepository.existsByKeyValue("alice@example.com")).thenReturn(true);

        CreatePixKeyRequest request = new CreatePixKeyRequest(account.getId(), PixKeyType.EMAIL, "alice@example.com");

        assertThatThrownBy(() -> pixKeyService.create(request))
                .isInstanceOf(PixKeyAlreadyExistsException.class);
    }

    @Test
    void deveLancarExcecaoQuandoLimiteDeChavesExcedido() {
        when(accountService.findById(account.getId())).thenReturn(account);
        when(pixKeyRepository.existsByKeyValue("alice@example.com")).thenReturn(false);
        when(pixKeyRepository.countByAccountId(account.getId())).thenReturn(5L);

        CreatePixKeyRequest request = new CreatePixKeyRequest(account.getId(), PixKeyType.EMAIL, "alice@example.com");

        assertThatThrownBy(() -> pixKeyService.create(request))
                .isInstanceOf(MaxPixKeysExceededException.class);
    }

    @Test
    void deveLancarExcecaoQuandoFormatoDeChaveInvalido() {
        when(accountService.findById(account.getId())).thenReturn(account);

        CreatePixKeyRequest request = new CreatePixKeyRequest(account.getId(), PixKeyType.CPF, "123");

        assertThatThrownBy(() -> pixKeyService.create(request))
                .isInstanceOf(com.portfolio.pix.exception.InvalidPixKeyFormatException.class);
    }
}
