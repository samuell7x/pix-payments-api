package com.portfolio.pix.service;

import com.portfolio.pix.dto.request.CreatePixKeyRequest;
import com.portfolio.pix.entity.Account;
import com.portfolio.pix.entity.PixKey;
import com.portfolio.pix.entity.PixKeyType;
import com.portfolio.pix.exception.MaxPixKeysExceededException;
import com.portfolio.pix.exception.PixKeyAlreadyExistsException;
import com.portfolio.pix.exception.PixKeyNotFoundException;
import com.portfolio.pix.repository.PixKeyRepository;
import com.portfolio.pix.validation.PixKeyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PixKeyService {

    // Regra real do Bacen: maximo de 5 chaves por conta de pessoa fisica.
    private static final int MAX_KEYS_PER_ACCOUNT = 5;

    private final PixKeyRepository pixKeyRepository;
    private final AccountService accountService;
    private final PixKeyValidator validator;

    @Transactional
    public PixKey create(CreatePixKeyRequest request) {
        Account account = accountService.findById(request.accountId());

        String keyValue = request.keyType() == PixKeyType.EVP
                ? UUID.randomUUID().toString()
                : request.keyValue();

        validator.validate(request.keyType(), keyValue);

        if (pixKeyRepository.existsByKeyValue(keyValue)) {
            throw new PixKeyAlreadyExistsException(keyValue);
        }

        if (pixKeyRepository.countByAccountId(account.getId()) >= MAX_KEYS_PER_ACCOUNT) {
            throw new MaxPixKeysExceededException(MAX_KEYS_PER_ACCOUNT);
        }

        PixKey key = PixKey.builder()
                .keyType(request.keyType())
                .keyValue(keyValue)
                .account(account)
                .build();

        try {
            return pixKeyRepository.save(key);
        } catch (DataIntegrityViolationException e) {
            // corrida entre duas criacoes concorrentes da mesma chave
            throw new PixKeyAlreadyExistsException(keyValue);
        }
    }

    @Transactional(readOnly = true)
    public PixKey findByKeyValue(String keyValue) {
        return pixKeyRepository.findByKeyValue(keyValue)
                .orElseThrow(() -> new PixKeyNotFoundException(keyValue));
    }

    @Transactional(readOnly = true)
    public List<PixKey> listByAccount(UUID accountId) {
        accountService.findById(accountId); // garante que a conta existe
        return pixKeyRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }
}
