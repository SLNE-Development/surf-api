package dev.slne.surf.database.reliability.internal

import dev.slne.surf.database.reliability.operation.AtomicOperationScope
import dev.slne.surf.database.reliability.operation.OperationKey
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction

/**
 * [AtomicOperationScope] of one execution attempt.
 */
internal class ActiveOperationScope(
    override val key: OperationKey,
    override val transaction: R2dbcTransaction,
) : AtomicOperationScope
