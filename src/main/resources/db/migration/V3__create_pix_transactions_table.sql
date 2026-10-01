CREATE TABLE pix_transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key   VARCHAR(100)    NOT NULL,
    source_account_id UUID            NOT NULL REFERENCES accounts(id),
    target_account_id UUID            NOT NULL REFERENCES accounts(id),
    target_pix_key    VARCHAR(255)    NOT NULL,
    amount            NUMERIC(19, 2)  NOT NULL CHECK (amount > 0),
    description       VARCHAR(280),
    status            VARCHAR(20)     NOT NULL,
    failure_reason    VARCHAR(100),
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ,

    CONSTRAINT uq_pix_tx_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_pix_tx_status CHECK (status IN ('COMPLETED','FAILED'))
);

CREATE INDEX idx_pix_tx_source_created ON pix_transactions (source_account_id, created_at DESC);
CREATE INDEX idx_pix_tx_target_created ON pix_transactions (target_account_id, created_at DESC);
