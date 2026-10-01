# pix-payments-api

Simulador de um PSP (provedor de serviço de pagamento) com PIX. Fiz este projeto para
praticar os problemas que aparecem quando duas transferências mexem na mesma conta ao
mesmo tempo: concorrência, idempotência e consistência no banco.

Não é uma integração com o SPI do Bacen. O registro de chaves e a liquidação entre
contas são implementações próprias, só para estudo.

## O que tem

- Contas com saldo
- Chaves PIX (CPF, CNPJ, e-mail, telefone e chave aleatória), com limite de 5 por conta
- Consulta de chave que devolve só o nome mascarado do titular
- Transferência entre contas com `Idempotency-Key`
- Extrato paginado, com filtro por direção (enviados/recebidos) e período
- Front-end em React para usar tudo isso pelo navegador

## Stack

- Java 25 e Spring Boot 3.5.16
- PostgreSQL 16 com Flyway
- Spring Data JPA
- springdoc-openapi (Swagger)
- JUnit 5, Mockito e Testcontainers
- React, TypeScript e Vite (pasta `frontend/`)
- Docker e Docker Compose

## Como a transferência funciona

O código fica em `PixTransferService`. Os três pontos que mais deram trabalho:

**Concorrência.** O débito e o crédito rodam numa transação só, e as duas contas são
travadas com `SELECT ... FOR UPDATE`. O lock é sempre adquirido na mesma ordem (pelo
UUID da conta). Sem isso, uma transferência A→B e outra B→A ao mesmo tempo podem se
travar uma na outra.

**Idempotência.** A chave `Idempotency-Key` tem uma constraint `UNIQUE` no banco. Se o
cliente reenviar a mesma requisição, a colisão é tratada e a API devolve a transação
que já tinha sido processada, sem debitar de novo. Deixei a garantia no banco, e não só
na aplicação, porque duas requisições iguais podem chegar juntas.

**Saldo insuficiente.** Não é erro do servidor. A transação é gravada com status
`FAILED` e o motivo, e a resposta explica o que houve.

O extrato usa `Specification` para combinar os filtros, em vez de uma sequência de
`if`.

## Como rodar

### Pré-requisitos

- JDK 25
- Docker
- Node 20 ou superior (só para o front-end)

### 1. Variáveis de ambiente

```powershell
copy .env.example .env
```

O `.env.example` já funciona para uso local. Se trocar a senha, troque nos dois campos
(`POSTGRES_PASSWORD` e `SPRING_DATASOURCE_PASSWORD`) e use o mesmo valor.

### 2. Tudo no Docker

```powershell
docker compose up --build
```

A API sobe em `http://localhost:8080` e o Swagger em `http://localhost:8080/docs`.

### 3. API pela IDE, banco no Docker

```powershell
docker compose up -d db
.\mvnw spring-boot:run
```

Por padrão a aplicação conecta em `localhost:5432`. Se essa porta estiver ocupada,
mude o mapeamento do serviço `db` no `docker-compose.yml` e informe a nova URL em
`SPRING_DATASOURCE_URL`, por exemplo `jdbc:postgresql://localhost:5433/pixdb`.

O `mvnw` baixa o Maven na primeira execução, então precisa de internet.

### 4. Front-end

Com a API no ar:

```powershell
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. Em desenvolvimento, o Vite encaminha `/api` para
`localhost:8080`, então não precisa de configuração de CORS.

## Testes

```powershell
.\mvnw test
```

Os testes de integração usam Testcontainers, então o Docker precisa estar rodando.

- `PixTransferServiceTest` e `PixKeyServiceTest`: regras de negócio com Mockito (saldo
  insuficiente, chave inexistente, transferência para a própria conta, reenvio
  idempotente, limite de chaves).
- `PixTransferConcurrencyIT`: sobe um PostgreSQL real e dispara 50 transferências
  simultâneas, com virtual threads, da mesma origem para o mesmo destino. O teste
  confere se o saldo final bate exatamente.

## Exemplos de uso

No PowerShell, use `curl.exe` em vez de `curl` (o `curl` do PowerShell é um alias de
outro comando e não aceita esses parâmetros).

Criar duas contas:

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerName":"Alice Silva","ownerDocument":"11111111111","initialBalance":500.00}'

curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerName":"Bruno Souza","ownerDocument":"22222222222","initialBalance":0}'
```

Cadastrar uma chave para o Bruno:

```bash
curl -X POST http://localhost:8080/api/v1/pix-keys \
  -H "Content-Type: application/json" \
  -d '{"accountId":"<id-do-bruno>","keyType":"EMAIL","keyValue":"bruno@example.com"}'
```

Transferir da Alice para o Bruno:

```bash
curl -X POST http://localhost:8080/api/v1/pix/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 9f1c1e2a-0000-4000-8000-abcdefabcdef" \
  -d '{"sourceAccountId":"<id-da-alice>","targetPixKey":"bruno@example.com","amount":150.00,"description":"aluguel"}'
```

Repetir essa requisição com o mesmo `Idempotency-Key` devolve a mesma transação, sem
debitar de novo.

Consultar o extrato:

```bash
curl "http://localhost:8080/api/v1/accounts/<id-da-alice>/statement?direction=OUT&page=0&size=10"
```

## Formato dos erros

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

A resposta não inclui stack trace nem detalhes internos.

## O que ficou de fora

- **Autenticação.** Qualquer `accountId` pode ser usado como origem. Com autenticação,
  a origem viria do usuário logado e não do corpo da requisição.
- **Validação de CPF e CNPJ.** Só confere a quantidade de dígitos, sem o dígito
  verificador.
- **Estorno e webhooks.** Ficaram para uma próxima etapa.
- **Rate limiting.**

Como não tem autenticação nem limite de requisições, o projeto serve para estudo e
demonstração. Não foi feito para ficar exposto na internet.
