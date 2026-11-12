package dev.slne.surf.database.reliability.operation

import java.io.Serial

/**
 * Thrown when [key] stayed locked by a concurrent execution for longer than
 * [AtomicOperationsConfig.lockTimeout]. The operation was not executed; a later attempt receives the reply of the
 * concurrent execution once it has committed.
 */
class AtomicOperationInProgressException(
    val key: OperationKey,
    cause: Throwable,
) : RuntimeException("Operation $key is locked by a concurrent execution", cause) {
    private companion object {
        @Serial
        private const val serialVersionUID: Long = -773202500274646494L
    }
}
