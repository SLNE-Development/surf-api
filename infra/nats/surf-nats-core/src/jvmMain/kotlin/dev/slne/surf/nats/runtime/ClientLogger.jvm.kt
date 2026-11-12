package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import net.kyori.adventure.text.logger.slf4j.ComponentLogger

@InternalNatsApi
internal actual class ClientLogger actual constructor(name: String) {
    private val logger = ComponentLogger.logger(name)

    actual fun debug(message: String, cause: Throwable?) = logger.debug(message, cause)

    actual fun info(message: String, cause: Throwable?) = logger.info(message, cause)

    actual fun warn(message: String, cause: Throwable?) = logger.warn(message, cause)

    actual fun error(message: String, cause: Throwable?) = logger.error(message, cause)
}
