package dev.slne.surf.microservice.runtime.command.builtin

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommands
import dev.slne.surf.microservice.runtime.health.MicroserviceHealth
import dev.slne.surf.microservice.runtime.spark.MicroserviceSpark

/**
 * Creates the commands every microservice has: `exit`, `health`, `help` and `spark`.
 */
internal fun builtinCommands(
    health: MicroserviceHealth,
    spark: MicroserviceSpark,
): List<MicroserviceCommand> {
    return listOf(ExitCommand(), HealthCommand(health), HelpCommand(), SparkCommand(spark))
}

/**
 * Registers each of [commands] in [MicroserviceCommands] unless its name or one of its aliases is
 * taken already.
 *
 * @return the skipped commands, each with the first of its labels that was taken
 */
internal fun registerBuiltinCommands(
    commands: List<MicroserviceCommand>,
): Map<MicroserviceCommand, String> {
    val skipped = LinkedHashMap<MicroserviceCommand, String>()

    for (command in commands) {
        MicroserviceCommands.registerIfAbsent(command)?.let { takenLabel ->
            skipped[command] = takenLabel
        }
    }

    return skipped
}
