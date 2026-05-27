package dev.slne.surf.api.paper.command.requirement

import dev.jorel.commandapi.ExecutableCommand
import dev.slne.surf.api.paper.command.executors.CoroutineScopeProvider
import dev.slne.surf.api.paper.command.executors.extractCallingPluginScopeOrThrow
import kotlinx.coroutines.CoroutineScope
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun <Impl : ExecutableCommand<Impl, CommandSender>> ExecutableCommand<Impl, CommandSender>.withSuspendPlayerRequirement(
    scope: CoroutineScopeProvider = extractCallingPluginScopeOrThrow(),
    allowIfNonPlayer: Boolean = false,
    requirement: suspend CoroutineScope.(Player) -> Boolean
) {
    SuspendRequirementService.instance.apply {
        withSuspendRequirement(scope, requirement, allowIfNonPlayer)
    }
}