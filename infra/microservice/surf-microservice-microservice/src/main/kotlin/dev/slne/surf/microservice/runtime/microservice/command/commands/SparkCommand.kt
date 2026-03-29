package dev.slne.surf.microservice.runtime.microservice.command.commands

import dev.slne.surf.microservice.api.microservice.command.microserviceCommand
import dev.slne.surf.microservice.runtime.microservice.spark.MicroserviceSpark
import me.lucko.spark.standalone.StandaloneCommandSender

fun sparkCommand() = microserviceCommand("spark") { args ->
    MicroserviceSpark.platform.executeCommand(
        StandaloneCommandSender.SYSTEM_OUT,
        args.toTypedArray()
    )
}