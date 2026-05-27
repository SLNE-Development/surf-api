package dev.slne.surf.api.paper.command.requirement

import dev.jorel.commandapi.ExecutableCommand
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.command.executors.CoroutineScopeProvider
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.CoroutineScope
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@InternalSurfApi
interface SuspendRequirementService {

    fun <Impl : ExecutableCommand<Impl, CommandSender>> ExecutableCommand<Impl, CommandSender>.withSuspendRequirement(
        scope: CoroutineScopeProvider,
        requirement: suspend CoroutineScope.(Player) -> Boolean,
        allowIfNonPlayer: Boolean
    )

    companion object {
        val instance = requiredService<SuspendRequirementService>()
    }
}