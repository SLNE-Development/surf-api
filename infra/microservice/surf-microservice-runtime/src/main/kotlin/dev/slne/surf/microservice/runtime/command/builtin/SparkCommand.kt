package dev.slne.surf.microservice.runtime.command.builtin

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.microservice.api.command.MicroserviceCommandOutput
import dev.slne.surf.microservice.runtime.spark.MicroserviceSpark
import dev.slne.surf.microservice.runtime.spark.MicroserviceSparkCommandSender

/**
 * Runs a spark command with the given arguments, for example `spark health`, and sends its output to the command
 * output. Fails while spark is not running.
 */
internal class SparkCommand(
    private val spark: MicroserviceSpark,
) : MicroserviceCommand("spark") {
    override val description = "Runs a spark profiler command."

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        if (!spark.isRunning) fail("spark is not running.")
        spark.executeCommand(MicroserviceSparkCommandSender(ContextOutput(this)), args)
    }

    private class ContextOutput(private val context: MicroserviceCommandContext) : MicroserviceCommandOutput {
        override fun sendLine(message: String) = context.sendLine(message)

        override fun sendError(message: String) = context.sendError(message)
    }
}
