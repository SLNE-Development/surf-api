package dev.slne.surf.microservice.runtime.command

import dev.slne.surf.microservice.api.command.MicroserviceCommandOutput
import java.util.concurrent.CopyOnWriteArrayList

class RecordingCommandOutput : MicroserviceCommandOutput {
    val lines = CopyOnWriteArrayList<String>()
    val errors = CopyOnWriteArrayList<String>()

    override fun sendLine(message: String) {
        lines += message
    }

    override fun sendError(message: String) {
        errors += message
    }
}
