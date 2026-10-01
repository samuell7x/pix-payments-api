package com.portfolio.pix.controller;

import com.portfolio.pix.dto.request.PixTransferRequest;
import com.portfolio.pix.dto.response.PixTransferResponse;
import com.portfolio.pix.entity.PixTransaction;
import com.portfolio.pix.entity.TransactionStatus;
import com.portfolio.pix.exception.MissingIdempotencyKeyException;
import com.portfolio.pix.service.PixTransferService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pix/transfers")
@RequiredArgsConstructor
public class PixTransferController {

    private final PixTransferService pixTransferService;

    @PostMapping
    public ResponseEntity<PixTransferResponse> transfer(
            @Valid @RequestBody PixTransferRequest request,
            @Parameter(description = "Chave de idempotencia unica por tentativa de transferencia")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (!StringUtils.hasText(idempotencyKey)) {
            throw new MissingIdempotencyKeyException();
        }

        PixTransaction transaction = pixTransferService.transfer(request, idempotencyKey);
        PixTransferResponse body = PixTransferResponse.from(transaction);

        // Falha de negocio (ex: saldo insuficiente) ainda e uma resposta
        // valida do dominio PIX, entao retorna 201 com o status refletido
        // no corpo - nao e um erro HTTP.
        HttpStatus status = transaction.getStatus() == TransactionStatus.COMPLETED
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(body);
    }
}
