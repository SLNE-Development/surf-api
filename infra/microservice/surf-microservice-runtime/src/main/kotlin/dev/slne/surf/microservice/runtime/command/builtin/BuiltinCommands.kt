package dev.slne.surf.microservice.runtime.command.builtin

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.runtime.health.MicroserviceHealthManager
import dev.slne.surf.microservice.runtime.spark.MicroserviceSpark

/**
 * Creates the commands every microservice has: `exit`, `health`, `help` and `spark`.
 */
internal fun builtinCommands(
    healthManager: MicroserviceHealthManager,
    spark: MicroserviceSpark,
): List<MicroserviceCommand> = listOf(ExitCommand(), HealthCommand(healthManager), HelpCommand(), SparkCommand(spark))
