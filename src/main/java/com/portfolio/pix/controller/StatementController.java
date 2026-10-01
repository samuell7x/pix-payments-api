package com.portfolio.pix.controller;

import com.portfolio.pix.dto.response.PageResponse;
import com.portfolio.pix.dto.response.PixTransferResponse;
import com.portfolio.pix.entity.PixTransaction;
import com.portfolio.pix.service.StatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/statement")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    @GetMapping
    public ResponseEntity<PageResponse<PixTransferResponse>> getStatement(
            @PathVariable UUID accountId,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safeSize = Math.min(size, 100); // protege contra paginas absurdamente grandes
        PageRequest pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PixTransaction> result = statementService.getStatement(accountId, direction, from, to, pageable);
        Page<PixTransferResponse> mapped = result.map(PixTransferResponse::from);

        return ResponseEntity.ok(PageResponse.from(mapped));
    }
}
