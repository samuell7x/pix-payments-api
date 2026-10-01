package com.portfolio.pix.controller;

import com.portfolio.pix.dto.request.CreatePixKeyRequest;
import com.portfolio.pix.dto.response.PixKeyLookupResponse;
import com.portfolio.pix.dto.response.PixKeyResponse;
import com.portfolio.pix.entity.PixKey;
import com.portfolio.pix.service.PixKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pix-keys")
@RequiredArgsConstructor
public class PixKeyController {

    private final PixKeyService pixKeyService;

    @PostMapping
    public ResponseEntity<PixKeyResponse> create(@Valid @RequestBody CreatePixKeyRequest request) {
        PixKey key = pixKeyService.create(request);
        PixKeyResponse body = PixKeyResponse.from(key);
        return ResponseEntity.created(URI.create("/api/v1/pix-keys/" + key.getKeyValue())).body(body);
    }

    /**
     * Simula a consulta ao DICT: dado um valor de chave, retorna apenas
     * o suficiente para o pagador confirmar o destinatario antes de
     * transferir - nunca saldo ou documento completo.
     */
    @GetMapping("/{keyValue}")
    public ResponseEntity<PixKeyLookupResponse> lookup(@PathVariable String keyValue) {
        PixKey key = pixKeyService.findByKeyValue(keyValue);
        return ResponseEntity.ok(PixKeyLookupResponse.from(key));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<PixKeyResponse>> listByAccount(@PathVariable UUID accountId) {
        List<PixKeyResponse> keys = pixKeyService.listByAccount(accountId).stream()
                .map(PixKeyResponse::from)
                .toList();
        return ResponseEntity.ok(keys);
    }
}
