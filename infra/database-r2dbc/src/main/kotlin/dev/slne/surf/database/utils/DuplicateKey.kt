package dev.slne.surf.database.utils

import dev.slne.surf.database.reliability.internal.SqlFailure

/**
 * Returns whether this failure is a unique or primary key violation: SQLSTATE `23505` on PostgreSQL, error code
 * `1062` on MariaDB.
 *
 * Inspects the first `R2dbcException` in the cause chain that carries a SQLSTATE or vendor code, so it works on the
 * `ExposedR2dbcException` thrown by a statement and on the driver exception alike. Unlike
 * [isDataIntegrityViolation], it does not match foreign key, `NOT NULL` or check constraint violations.
 *
 * On PostgreSQL the failed statement aborts the transaction, so a caller that handles the duplicate must retry in a
 * new transaction. To insert a row only if its key is absent, Exposed's `insertIgnore` keeps the transaction usable.
 */
fun Throwable.isDuplicateKeyViolation(): Boolean = SqlFailure.of(this) == SqlFailure.DUPLICATE_KEY
