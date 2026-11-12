package dev.slne.surf.database.reliability.internal

import org.jetbrains.exposed.v1.core.Table

/**
 * Mapping of `surf_atomic_operations`; the schema itself comes from the library's migrations.
 */
internal object AtomicOperationTable : Table("surf_atomic_operations") {
    val owner = varchar("owner", 64)
    val operation = varchar("operation", 255)
    val idempotencyKey = varchar("idempotency_key", 255)
    val requestFingerprint = registerColumn("request_fingerprint", ByteArrayColumnType())
    val responseFormat = varchar("response_format", 255).nullable()
    val responseBytes = registerColumn("response_bytes", ByteArrayColumnType()).nullable()
    val createdAt = registerColumn("created_at", UtcInstantColumnType())
    val completedAt = registerColumn("completed_at", UtcInstantColumnType()).nullable()
    val expiresAt = registerColumn("expires_at", UtcInstantColumnType())

    override val primaryKey = PrimaryKey(owner, operation, idempotencyKey)
}
