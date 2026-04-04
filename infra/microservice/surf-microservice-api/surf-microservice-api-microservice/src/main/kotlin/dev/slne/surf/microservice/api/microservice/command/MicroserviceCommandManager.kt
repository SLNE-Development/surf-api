package dev.slne.surf.microservice.api.microservice.command

import dev.slne.surf.api.core.util.requiredService
import it.unimi.dsi.fastutil.objects.ObjectList
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.UnmodifiableView

private val commandManager = requiredService<MicroserviceCommandManager>()

interface MicroserviceCommandManager {
    val commands: @UnmodifiableView ObjectList<MicroserviceCommand>

    fun findCommand(label: String): MicroserviceCommand?
    fun registerCommand(command: MicroserviceCommand)

    fun handleCommand(input: String, scope: CoroutineScope)

    companion object : MicroserviceCommandManager by commandManager {
        val INSTANCE get() = commandManager
    }
}