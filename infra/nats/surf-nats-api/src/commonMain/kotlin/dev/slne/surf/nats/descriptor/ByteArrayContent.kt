package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi

@NatsGeneratedApi
public fun natsContentEquals(a: ByteArray?, b: ByteArray?): Boolean = a.contentEquals(b)

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

@NatsGeneratedApi
public fun natsContentHashCode(value: ByteArray?): Int = value.contentHashCode()

@NatsGeneratedApi
public fun natsContentHashCode(value: List<ByteArray?>?): Int {
    if (value == null) return 0

    var result = 1
    for (element in value) {
        result = 31 * result + element.contentHashCode()
    }
    return result
}

@NatsGeneratedApi
public fun natsContentHashCode(value: Map<*, ByteArray?>?): Int {
    if (value == null) return 0

    var result = 0
    for ((key, element) in value) {
        result += key.hashCode() xor element.contentHashCode()
    }
    return result
}

@NatsGeneratedApi
public fun natsContentToString(value: ByteArray?): String = value.contentToString()

@NatsGeneratedApi
public fun natsContentToString(value: List<ByteArray?>?): String {
    if (value == null) return "null"
    return value.joinToString(", ", "[", "]") { it.contentToString() }
}

@NatsGeneratedApi
public fun natsContentToString(value: Map<*, ByteArray?>?): String {
    if (value == null) return "null"
    return value.entries.joinToString(
        ", ",
        "{",
        "}"
    ) { (key, element) -> "$key=${element.contentToString()}" }
}
