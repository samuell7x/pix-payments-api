# pix-payments-api

Simulador de PSP (Provedor de Serviço de Pagamento) com PIX, focado em demonstrar
domínio dos problemas reais de engenharia que um sistema de transferência instantânea
exige: **controle de concorrência**, **idempotência** e **consistência transacional**.

> ⚠️ Este NÃO é uma integração com o SPI/Bacen. É um simulador de portfólio, com um
> "DICT" (registro de chaves) e uma "liquidação" (transferência entre contas) próprios.

## Stack

- Java 25 + Spring Boot 3.5.16
- PostgreSQL 16 + Flyway
- Spring Data JPA (lock pessimista para concorrência)
- springdoc-openapi (Swagger UI)
- JUnit 5 + Mockito (unitários) + Testcontainers (integração/concorrência)
- Docker + Docker Compose

## Arquitetura

```
controller  → validação de entrada (Bean Validation), mapeamento HTTP
service     → regra de negócio, transação, locking pessimista, idempotência
repository  → Spring Data JPA + Specifications
PostgreSQL  → Flyway migrations
```

### Decisões de design relevantes

| Problema | Solução | Onde |
|---|---|---|
| Lost update entre débito e crédito | `SELECT ... FOR UPDATE` nas duas contas, **sempre na mesma ordem de UUID** (evita deadlock entre transferências opostas A→B e B→A simultâneas) | `PixTransferService`, `AccountRepository.findByIdForUpdate` |
| Retry de rede duplicando pagamento | `UNIQUE` constraint em `idempotency_key`; ao colidir, retorna a transação já processada em vez de reprocessar | `PixTransferService.persistIdempotently` |
| Saldo insuficiente não é bug | É um resultado de negócio válido: a transação é **persistida como `FAILED`** com motivo, não vira exceção 500 | `PixTransferService.transfer` |
| Consulta de chave não pode vazar saldo | Endpoint de lookup retorna só nome do titular mascarado | `PixKeyLookupResponse` |
| Extrato com muitos filtros | `Specification` composable em vez de if-chain gigante | `PixTransactionSpecifications` |

## Simplificações assumidas (fora de escopo deste portfólio)

- **Sem autenticação/autorização.** Qualquer `accountId` pode ser usado como origem.
  Numa evolução real, entraria Spring Security + JWT, e `sourceAccountId` viria do
  contexto autenticado, nunca do corpo da requisição.
- **Validação de CPF/CNPJ é apenas de formato** (quantidade de dígitos), sem o
  dígito verificador (mod 11) do documento real.
- **Sem webhook/callback assíncrono** de notificação — pode ser adicionado depois
  como endpoint de callback configurável por conta.
- **Sem devolução/estorno de PIX** — evolução natural do MVP.

## Como rodar

### 1. Configurar variáveis de ambiente

```powershell
copy .env.example .env
```

Edite o `.env` e defina uma senha (não use a senha de exemplo).

### 2. Subir com Docker Compose

```powershell
docker compose up --build
```

A API sobe em `http://localhost:8080`. Documentação interativa em
`http://localhost:8080/docs`.

### 3. Rodar localmente sem Docker (opcional)

```powershell
.\mvnw spring-boot:run
```

> A primeira execução do `mvnw` baixa o Maven Wrapper e o Maven automaticamente —
> precisa de internet nesse primeiro passo. Requer JDK 25 e um PostgreSQL local
> (ou suba só o banco com `docker compose up db`).

## Testes

```powershell
.\mvnw test
```

- **Unitários** (`PixTransferServiceTest`, `PixKeyServiceTest`): regras de negócio
  isoladas com Mockito — saldo insuficiente, chave inexistente, auto-transferência,
  replay idempotente, limite de chaves por conta.
- **Integração** (`PixTransferConcurrencyIT`): sobe um PostgreSQL real via
  Testcontainers e dispara **50 transferências concorrentes** (virtual threads) da
  mesma conta de origem para a mesma conta de destino, validando que o saldo final
  é matematicamente exato — prova de que o locking elimina lost updates.

## Fluxo de uso (exemplos)

### Criar duas contas

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerName":"Alice Silva","ownerDocument":"11111111111","initialBalance":500.00}'

curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerName":"Bruno Souza","ownerDocument":"22222222222","initialBalance":0}'
```

### Cadastrar uma chave PIX para o Bruno

```bash
curl -X POST http://localhost:8080/api/v1/pix-keys \
  -H "Content-Type: application/json" \
  -d '{"accountId":"<id-do-bruno>","keyType":"EMAIL","keyValue":"bruno@example.com"}'
```

### Transferir da Alice para o Bruno

```bash
curl -X POST http://localhost:8080/api/v1/pix/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 9f1c1e2a-0000-4000-8000-abcdefabcdef" \
  -d '{"sourceAccountId":"<id-da-alice>","targetPixKey":"bruno@example.com","amount":150.00,"description":"aluguel"}'
```

Repetir a mesma requisição com o **mesmo** `Idempotency-Key` retorna a transação já
processada, sem duplicar o débito.

### Consultar extrato

```bash
curl "http://localhost:8080/api/v1/accounts/<id-da-alice>/statement?direction=OUT&page=0&size=10"
```

## Padrão de erro

```json
{
  "timestamp": "2026-09-11T23:40:00Z",
  "status": 422,
  "error": "BUSINESS_RULE_VIOLATION",
  "message": "Limite de 5 chaves PIX por conta foi atingido",
  "path": "/api/v1/pix-keys",
  "details": null
}
```

Nenhum erro expõe stack trace, causa interna ou detalhes de infraestrutura ao cliente.

## Próximas evoluções sugeridas

1. Spring Security + JWT (remover `sourceAccountId` do corpo, extrair do contexto)
2. Devolução/estorno de PIX dentro de janela de tempo
3. Webhook HTTP configurável por conta para notificação assíncrona de status
4. Rate limiting por conta/IP (proteção contra brute force de valores)
5. Cálculo real do dígito verificador de CPF/CNPJ
