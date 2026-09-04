package dev.slne.surf.api.minestom.command.commandapi.executor

import kotlinx.coroutines.CoroutineScope
import net.minestom.server.command.CommandSender
import net.minestom.server.command.ConsoleSender
import net.minestom.server.entity.Player
import org.jetbrains.annotations.ApiStatus

enum class ExecutorType {
    ANY,
    PLAYER,
    CONSOLE,
}

data class ExecutionInfo<S : CommandSender>(
    val sender: S,
    val args: CommandArguments,
    val input: String,
)

sealed interface ExecutorDefinition {
    val type: ExecutorType

    data class Normal(
        override val type: ExecutorType,
        val executor: (ExecutionInfo<CommandSender>) -> Unit,
    ) : ExecutorDefinition

    data class Resulting(
        override val type: ExecutorType,
        val executor: (ExecutionInfo<CommandSender>) -> Int,
    ) : ExecutorDefinition

    data class Suspending(
        override val type: ExecutorType,
        val scopeProvider: () -> CoroutineScope,
        val executor: suspend CoroutineScope.(ExecutionInfo<CommandSender>) -> Unit,
    ) : ExecutorDefinition
}

interface CommandExecutable<SELF : CommandExecutable<SELF>> {
    @ApiStatus.Internal
    fun addExecutor(definition: ExecutorDefinition): SELF

    fun executes(executor: (CommandSender, CommandArguments) -> Unit): SELF = addExecutor(
        ExecutorDefinition.Normal(ExecutorType.ANY) { info -> executor(info.sender, info.args) },
    )

    fun executesPlayer(executor: (Player, CommandArguments) -> Unit): SELF = addExecutor(
        ExecutorDefinition.Normal(ExecutorType.PLAYER) { info ->
            val player = info.sender
            check(player is Player) { "PLAYER executor received ${player::class.qualifiedName}" }
            executor(player, info.args)
        },
    )

    fun executesConsole(executor: (ConsoleSender, CommandArguments) -> Unit): SELF = addExecutor(
        ExecutorDefinition.Normal(ExecutorType.CONSOLE) { info ->
            val console = info.sender
            check(console is ConsoleSender) { "CONSOLE executor received ${console::class.qualifiedName}" }
            executor(console, info.args)
        },
    )
}
