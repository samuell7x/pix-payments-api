package com.portfolio.pix.integration;

import com.portfolio.pix.dto.request.PixTransferRequest;
import com.portfolio.pix.entity.Account;
import com.portfolio.pix.entity.PixKey;
import com.portfolio.pix.entity.PixKeyType;
import com.portfolio.pix.repository.AccountRepository;
import com.portfolio.pix.repository.PixKeyRepository;
import com.portfolio.pix.service.PixTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova sob carga real (Postgres via Testcontainers + virtual threads)
 * de que o lock pessimista ordenado por ID elimina lost updates: N
 * transferencias concorrentes da mesma conta de origem para a mesma
 * conta de destino devem resultar em saldo final EXATO, sem corrida.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class PixTransferConcurrencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pixdb_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PixKeyRepository pixKeyRepository;
    @Autowired
    private PixTransferService pixTransferService;

    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        source = accountRepository.save(Account.builder()
                .ownerName("Origem Concorrente").ownerDocument(randomDocument())
                .balance(new BigDecimal("1000.00")).build());
        target = accountRepository.save(Account.builder()
                .ownerName("Destino Concorrente").ownerDocument(randomDocument())
                .balance(BigDecimal.ZERO).build());
        pixKeyRepository.save(PixKey.builder()
                .keyType(PixKeyType.EVP).keyValue(UUID.randomUUID().toString()).account(target).build());
    }

    @Test
    void deveManterConsistenciaComTransferenciasConcorrentes() throws InterruptedException {
        String targetKeyValue = pixKeyRepository.findByAccountIdOrderByCreatedAtDesc(target.getId())
                .get(0).getKeyValue();

        int concurrentTransfers = 50;
        BigDecimal amountPerTransfer = new BigDecimal("10.00");

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(concurrentTransfers);

            IntStream.range(0, concurrentTransfers).forEach(i -> executor.submit(() -> {
                try {
                    startLatch.await();
                    PixTransferRequest request = new PixTransferRequest(
                            source.getId(), targetKeyValue, amountPerTransfer, "teste-concorrencia-" + i);
                    pixTransferService.transfer(request, "idem-concurrency-" + i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }));

            startLatch.countDown();
            boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
            assertThat(finished).isTrue();
        }

        Account finalSource = accountRepository.findById(source.getId()).orElseThrow();
        Account finalTarget = accountRepository.findById(target.getId()).orElseThrow();

        BigDecimal expectedTransferred = amountPerTransfer.multiply(BigDecimal.valueOf(concurrentTransfers));

        assertThat(finalSource.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00").subtract(expectedTransferred));
        assertThat(finalTarget.getBalance()).isEqualByComparingTo(expectedTransferred);
    }

    private String randomDocument() {
        return String.valueOf(System.nanoTime()).substring(0, 11);
    }
}
