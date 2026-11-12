package dev.slne.surf.microservice.runtime.command

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.microservice.api.command.MicroserviceCommandOutput

/**
 * Writes command output to the log: lines at info, errors at warning level.
 */
internal object LoggingCommandOutput : MicroserviceCommandOutput {
    private val log = logger()

    override fun sendLine(message: String) {
        log.atInfo().log("%s", message)
    }

    override fun sendError(message: String) {
        log.atWarning().log("%s", message)
    }
}
