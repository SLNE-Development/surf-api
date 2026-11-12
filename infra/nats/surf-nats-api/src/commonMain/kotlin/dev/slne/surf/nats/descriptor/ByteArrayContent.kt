package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi

/**
 * Checks whether [a] and [b] are both `null` or have equal contents.
 */
@NatsGeneratedApi
public fun natsContentEquals(a: ByteArray?, b: ByteArray?): Boolean = a.contentEquals(b)

/**
 * Checks whether [a] and [b] are both `null` or have the same size and pairwise equal byte array contents.
 */
@NatsGeneratedApi
public fun natsContentEquals(a: List<ByteArray?>?, b: List<ByteArray?>?): Boolean {
    if (a === b) return true
    if (a == null || b == null || a.size != b.size) return false

    val others = b.iterator()
    for (element in a) {
        if (!element.contentEquals(others.next())) return false
    }
    return true
}

/**
 * Checks whether [a] and [b] are both `null` or have the same keys with equal byte array contents.
 *
 * A key mapped to `null` differs from a missing key.
 */
@NatsGeneratedApi
public fun <K> natsContentEquals(a: Map<K, ByteArray?>?, b: Map<K, ByteArray?>?): Boolean {
    if (a === b) return true
    if (a == null || b == null || a.size != b.size) return false

    for ((key, value) in a) {
        val other = b[key]
        if (other == null) {
            if (value != null || !b.containsKey(key)) return false
        } else if (!value.contentEquals(other)) {
            return false
        }
    }
    return true
}

/**
 * Returns a hash code of the contents of [value], consistent with [natsContentEquals].
 */
@NatsGeneratedApi
public fun natsContentHashCode(value: ByteArray?): Int = value.contentHashCode()

/**
 * Returns a hash code of the byte array contents of [value], consistent with [natsContentEquals] and computed like
 * [List.hashCode].
 */
@NatsGeneratedApi
public fun natsContentHashCode(value: List<ByteArray?>?): Int {
    if (value == null) return 0

    var result = 1
    for (element in value) {
        result = 31 * result + element.contentHashCode()
    }
    return result
}

/**
 * Returns a hash code of the keys and byte array contents of [value], consistent with [natsContentEquals] and computed
 * like [Map.hashCode].
 */
@NatsGeneratedApi
public fun natsContentHashCode(value: Map<*, ByteArray?>?): Int {
    if (value == null) return 0

    var result = 0
    for ((key, element) in value) {
        result += key.hashCode() xor element.contentHashCode()
    }
    return result
}

/**
 * Returns the contents of [value] as a string, such as `[1, 2, 3]`, or the string `"null"` for a `null` value.
 */
@NatsGeneratedApi
public fun natsContentToString(value: ByteArray?): String = value.contentToString()

/**
 * Returns the byte array contents of [value] as a string, such as `[[1, 2], null]`, or the string `"null"` for a
 * `null` list.
 */
@NatsGeneratedApi
public fun natsContentToString(value: List<ByteArray?>?): String {
    if (value == null) return "null"
    return value.joinToString(", ", "[", "]") { it.contentToString() }
}

/**
 * Returns the keys and byte array contents of [value] as a string, such as `{a=[1, 2]}`, or the string `"null"` for
 * a `null` map.
 */
@NatsGeneratedApi
public fun natsContentToString(value: Map<*, ByteArray?>?): String {
    if (value == null) return "null"
    return value.entries.joinToString(
        ", ",
        "{",
        "}"
    ) { (key, element) -> "$key=${element.contentToString()}" }
}
