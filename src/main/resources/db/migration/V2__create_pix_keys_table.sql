CREATE TABLE pix_keys (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_type    VARCHAR(10)     NOT NULL,
    key_value   VARCHAR(255)    NOT NULL,
    account_id  UUID            NOT NULL REFERENCES accounts(id),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_pix_keys_key_value UNIQUE (key_value),
    CONSTRAINT chk_pix_keys_type CHECK (key_type IN ('CPF','CNPJ','EMAIL','PHONE','EVP'))
);

CREATE INDEX idx_pix_keys_account_id ON pix_keys (account_id);
