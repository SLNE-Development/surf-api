package dev.slne.surf.nats.client

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import java.time.Instant

/**
 * Records where a `NatsClient` was created, so clients left open at shutdown can be traced to their creator.
 *
 * [capture] skips the frames of the client factory and keeps at most 12 frames of the caller.
 */
@InternalNatsApi
internal class ClientOrigin private constructor(
    val createdAt: Instant,
    val thread: String,
    val source: String,
    val stackTrace: List<StackTraceElement>,
) {
    companion object {
        private const val MAX_FRAMES = 12L
        private val walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
        private val internalFrames = setOf(
            Companion::class.java,
            NatsClientFactoryImpl::class.java,
            NatsClient.Factory::class.java,
        )

        fun capture(): ClientOrigin {
            val frames = walker.walk { stream ->
                stream.dropWhile { it.declaringClass in internalFrames }
                    .limit(MAX_FRAMES)
                    .toList()
            }
            val caller = frames.firstOrNull()?.declaringClass

            return ClientOrigin(
                createdAt = Instant.now(),
                thread = Thread.currentThread().name,
                source = caller?.let(::sourceOf) ?: "unknown",
                stackTrace = frames.map { it.toStackTraceElement() },
            )
        }

        private fun sourceOf(type: Class<*>): String {
            val location = type.protectionDomain?.codeSource?.location ?: type.classLoader
            return "${type.name} ($location)"
        }
    }
}