CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_name      VARCHAR(150)    NOT NULL,
    owner_document  VARCHAR(14)     NOT NULL,
    balance         NUMERIC(19, 2)  NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_accounts_owner_document UNIQUE (owner_document)
);

CREATE EXTENSION IF NOT EXISTS pgcrypto;
