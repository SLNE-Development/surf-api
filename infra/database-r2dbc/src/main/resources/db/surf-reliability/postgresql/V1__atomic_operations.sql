-- Stored replies of idempotent operations. A row is committed only together with its reply, so
-- response_format, response_bytes and completed_at are NULL only inside the claiming transaction.
CREATE TABLE IF NOT EXISTS surf_atomic_operations (
    owner               VARCHAR(64)  NOT NULL,
    operation           VARCHAR(255) NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL,
    request_fingerprint BYTEA        NOT NULL,
    response_format     VARCHAR(255),
    response_bytes      BYTEA,
    created_at          TIMESTAMPTZ  NOT NULL,
    completed_at        TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT surf_atomic_operations_pkey PRIMARY KEY (owner, operation, idempotency_key)
);

CREATE INDEX IF NOT EXISTS surf_atomic_operations_expires_at
    ON surf_atomic_operations (expires_at);
