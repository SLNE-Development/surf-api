package dev.slne.surf.microservice.runtime.microservice.command.commands

import dev.slne.surf.microservice.api.microservice.command.microserviceCommand
import dev.slne.surf.microservice.runtime.microservice.spark.MicroserviceSpark
import dev.slne.surf.microservice.runtime.microservice.spark.MicroserviceSparkCommandSender
import kotlinx.coroutines.future.await
import me.lucko.spark.standalone.StandaloneCommandSender

fun sparkCommand() = microserviceCommand("spark") { args ->
    val sender = MicroserviceSparkCommandSender(this)

    MicroserviceSpark.executeCommand(
        sender,
        args.toTypedArray()
    ).await()
}