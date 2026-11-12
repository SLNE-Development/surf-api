package dev.slne.surf.database.reliability.operation

/**
 * Identity of one idempotent operation: at most one execution per key is committed.
 *
 * Keys are compared exactly, including case and trailing spaces. [owner] namespaces the shared tables per service,
 * [operation] names the operation (for example a stable RPC method name), and [idempotencyKey] identifies the
 * request, for example a message id.
 *
 * @throws IllegalArgumentException if a component is blank or longer than its column: [owner] 64,
 * [operation] and [idempotencyKey] 255 characters
 */
data class OperationKey(
    val owner: String,
    val operation: String,
    val idempotencyKey: String,
) {
    init {
        requireComponent("owner", owner, MAX_OWNER_LENGTH)
        requireComponent("operation", operation, MAX_OPERATION_LENGTH)
        requireComponent("idempotencyKey", idempotencyKey, MAX_IDEMPOTENCY_KEY_LENGTH)
    }

    private fun requireComponent(name: String, value: String, maxLength: Int) {
        require(value.isNotBlank()) { "$name must not be blank" }
        require(value.length <= maxLength) { "$name must have at most $maxLength characters, was ${value.length}" }
    }

    companion object {
        /**
         * Maximum length of [owner].
         */
        const val MAX_OWNER_LENGTH = 64

        /**
         * Maximum length of [operation].
         */
        const val MAX_OPERATION_LENGTH = 255

        /**
         * Maximum length of [idempotencyKey].
         */
        const val MAX_IDEMPOTENCY_KEY_LENGTH = 255
    }
}
