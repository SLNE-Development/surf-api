package dev.slne.surf.api.paper.command.requirement

import dev.jorel.commandapi.ExecutableCommand
import dev.slne.surf.api.paper.command.executors.CoroutineScopeProvider
import dev.slne.surf.api.paper.command.executors.extractCallingPluginScopeOrThrow
import kotlinx.coroutines.CoroutineScope
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Adds a suspendable player-only requirement to this command.
 *
 * The suspendable [requirement] is refreshed asynchronously when the command tree is sent
 * to a player. Its result is cached and used by the underlying CommandAPI requirement.
 *
 * This means the cached result is still checked when the command is executed, but the
 * suspendable requirement itself is not refreshed at execution time. The command may
 * therefore be executed based on the last cached value until the command tree is sent
 * again and the cache is updated.
 *
 * The cached result can be refreshed manually by calling [Player.updateCommands], which
 * causes the command tree to be resent to the player.
 *
 * Because of that, this should mainly be used for command visibility or other visual
 * command-list behavior. Any condition that must be enforced with up-to-date data during
 * execution should also be checked explicitly inside the command executor.
 *
 * @param scope the coroutine scope provider used to refresh the requirement
 * @param allowIfNonPlayer whether non-player command senders should bypass this player-only requirement
 * @param requirement the suspendable predicate used to refresh the cached requirement
 * result for a player
 */
fun <Impl : ExecutableCommand<Impl, CommandSender>> ExecutableCommand<Impl, CommandSender>.withSuspendPlayerRequirement(
    scope: CoroutineScopeProvider = extractCallingPluginScopeOrThrow(),
    allowIfNonPlayer: Boolean = false,
    requirement: suspend CoroutineScope.(Player) -> Boolean
) {
    SuspendRequirementService.instance.apply {
        withSuspendRequirement(scope, requirement, allowIfNonPlayer)
    }
}