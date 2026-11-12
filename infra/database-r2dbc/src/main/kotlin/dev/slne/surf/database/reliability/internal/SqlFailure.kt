package dev.slne.surf.database.reliability.internal

import io.r2dbc.spi.R2dbcException

/**
 * Database errors the reliability stores react to, recognized by SQLSTATE (PostgreSQL) or vendor code (MariaDB).
 *
 * Exposed wraps driver errors without SQLSTATE, and the drivers map the same event to different exception classes,
 * so [of] inspects the first [R2dbcException] in the cause chain that carries a state or code.
 */
internal enum class SqlFailure {
    DUPLICATE_KEY,
    LOCK_TIMEOUT,

    /**
     * Deadlock or serialization failure: the transaction was rolled back and can be retried as a whole.
     */
    TRANSIENT_CONFLICT;

    companion object {
        private const val PG_UNIQUE_VIOLATION = "23505"
        private const val PG_LOCK_NOT_AVAILABLE = "55P03"
        private const val PG_DEADLOCK_DETECTED = "40P01"
        private const val SERIALIZATION_FAILURE = "40001"
        private const val MARIADB_DUPLICATE_ENTRY = 1062
        private const val MARIADB_LOCK_WAIT_TIMEOUT = 1205
        private const val MARIADB_DEADLOCK = 1213

        fun of(failure: Throwable): SqlFailure? {
            val cause = generateSequence(failure) { it.cause }
                .filterIsInstance<R2dbcException>()
                .find { it.sqlState != null || it.errorCode != 0 }
                ?: return null

            return when {
                cause.sqlState == PG_UNIQUE_VIOLATION || cause.errorCode == MARIADB_DUPLICATE_ENTRY -> DUPLICATE_KEY
                cause.sqlState == PG_LOCK_NOT_AVAILABLE || cause.errorCode == MARIADB_LOCK_WAIT_TIMEOUT -> LOCK_TIMEOUT
                cause.sqlState == PG_DEADLOCK_DETECTED ||
                        cause.sqlState == SERIALIZATION_FAILURE ||
                        cause.errorCode == MARIADB_DEADLOCK -> TRANSIENT_CONFLICT

                else -> null
            }
        }
    }
}
