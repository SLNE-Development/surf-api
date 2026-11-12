-- Stored replies of idempotent operations. A row is committed only together with its reply, so
-- response_format, response_bytes and completed_at are NULL only inside the claiming transaction.
-- Keys use a binary NO PAD collation, as on PostgreSQL: keys are case-sensitive and trailing spaces
-- are significant.
CREATE TABLE IF NOT EXISTS surf_atomic_operations (
    owner               VARCHAR(64)   NOT NULL,
    operation           VARCHAR(255)  NOT NULL,
    idempotency_key     VARCHAR(255)  NOT NULL,
    request_fingerprint VARBINARY(64) NOT NULL,
    response_format     VARCHAR(255)  NULL,
    response_bytes      LONGBLOB      NULL,
    created_at          DATETIME(6)   NOT NULL,
    completed_at        DATETIME(6)   NULL,
    expires_at          DATETIME(6)   NOT NULL,
    PRIMARY KEY (owner, operation, idempotency_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_nopad_bin;

CREATE INDEX IF NOT EXISTS surf_atomic_operations_expires_at
    ON surf_atomic_operations (expires_at);
