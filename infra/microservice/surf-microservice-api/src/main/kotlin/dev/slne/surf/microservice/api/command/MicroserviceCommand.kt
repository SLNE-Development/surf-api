package dev.slne.surf.microservice.api.command

import dev.slne.surf.microservice.api.util.InternalMicroserviceApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun microserviceCommand(
    name: String,
    vararg aliases: String,
    block: suspend CoroutineScope.(args: List<String>) -> Unit
): MicroserviceCommand = object : MicroserviceCommand(name, *aliases) {
    override suspend fun CoroutineScope.execute(args: List<String>) {
        block(args)
    }
}.register()

abstract class MicroserviceCommand(
    val name: String,
    vararg val aliases: String
) {
    @InternalMicroserviceApi
    internal fun execute(scope: CoroutineScope, args: List<String>) {
        scope.launch {
            scope.execute(args)
        }
    }

    abstract suspend fun CoroutineScope.execute(args: List<String>)

    fun register(): MicroserviceCommand {
        MicroserviceCommandManager.registerCommand(this)

        return this
    }
}