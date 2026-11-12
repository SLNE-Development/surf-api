package dev.slne.surf.database.reliability.operation

/**
 * Encoded reply of an operation, stored with the operation and returned unchanged for every repeated execution.
 *
 * The library treats both values as opaque. Two replies are equal if [format] and the content of [bytes] are equal.
 * The array is not copied: do not modify it after passing it in.
 *
 * @property format Caller-defined name of the encoding, at most 255 characters, for example a content type.
 * @property bytes Encoded reply.
 * @throws IllegalArgumentException if [format] is blank or longer than 255 characters
 */
class StoredReply(
    val format: String,
    val bytes: ByteArray,
) {
    init {
        require(format.isNotBlank()) { "format must not be blank" }
        require(format.length <= MAX_FORMAT_LENGTH) {
            "format must have at most $MAX_FORMAT_LENGTH characters, was ${format.length}"
        }
    }

    override fun toString(): String = "StoredReply(format=$format, bytes=${bytes.size} bytes)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoredReply) return false

        if (format != other.format) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }

    companion object {
        /**
         * Maximum length of [format].
         */
        const val MAX_FORMAT_LENGTH = 255
    }
}
