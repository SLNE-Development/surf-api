package dev.slne.surf.database.reliability.operation

import java.io.Serial

/**
 * Thrown when [key] was already executed with a different request fingerprint, so the stored reply does not belong
 * to this request.
 */
class AtomicOperationConflictException(
    val key: OperationKey,
) : RuntimeException("Operation $key was already executed with a different request") {
    private companion object {
        @Serial
        private const val serialVersionUID: Long = 6519310664851006651L
    }
}
