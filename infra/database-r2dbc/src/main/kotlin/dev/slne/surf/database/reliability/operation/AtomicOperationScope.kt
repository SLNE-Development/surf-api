package dev.slne.surf.database.reliability.operation

import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction

/**
 * Receiver of an [AtomicOperations.execute] block, valid only while the block runs.
 */
interface AtomicOperationScope {
    /**
     * Key of the running operation.
     */
    val key: OperationKey

    /**
     * Transaction of the running operation. Statements in the block run in it implicitly; a nested
     * `suspendTransaction` on the same database joins it.
     */
    val transaction: R2dbcTransaction
}
