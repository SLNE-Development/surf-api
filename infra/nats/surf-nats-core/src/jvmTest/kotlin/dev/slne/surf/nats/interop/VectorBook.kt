package dev.slne.surf.nats.interop

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Serves [InteropVectors] by name and checks what the interop script sends back.
 *
 * The script handles one vector at a time: it fetches the vector, then echoes it. An echo is compared with the
 * [InteropVector.echo] of the last served vector; a mismatch is recorded and fails the echo call.
 */
internal class VectorBook(vectors: List<InteropVector>) {
    private val byName = vectors.associateBy(InteropVector::name)

    @Volatile
    private var current: InteropVector? = null

    private val echoCounts = ConcurrentHashMap<String, AtomicInteger>()

    val mismatches = ConcurrentLinkedQueue<String>()

    fun serve(name: String): InteropVector {
        val vector = requireNotNull(byName[name]) { "Unknown interop vector '$name'" }
        current = vector
        return vector
    }

    inline fun <reified T : Any> serveTyped(name: String): T {
        val vector = serve(name)
        check(!vector.raw) { "Vector $name is served by the raw responder" }
        return vector.value as T
    }

    fun <T : Any> echo(decoded: T): T {
        val vector = checkNotNull(current) { "Echo of $decoded before any vector was served" }
        if (decoded != vector.echo) {
            val mismatch = "Vector ${vector.name}: Kotlin decoded $decoded, expected ${vector.echo}"
            mismatches += mismatch
            throw IllegalStateException(mismatch)
        }
        echoCounts.computeIfAbsent(vector.name) { AtomicInteger() }.incrementAndGet()
        return decoded
    }

    fun echoCount(name: String): Int = echoCounts[name]?.get() ?: 0
}
