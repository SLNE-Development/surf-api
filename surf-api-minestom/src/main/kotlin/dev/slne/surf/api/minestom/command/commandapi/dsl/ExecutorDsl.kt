package dev.slne.surf.api.minestom.command.commandapi.dsl

import dev.slne.surf.api.minestom.command.commandapi.executor.CommandArguments
import dev.slne.surf.api.minestom.command.commandapi.executor.CommandExecutable
import dev.slne.surf.api.minestom.command.commandapi.executor.ExecutionInfo
import dev.slne.surf.api.minestom.command.commandapi.executor.ExecutorDefinition
import dev.slne.surf.api.minestom.command.commandapi.executor.ExecutorType
import dev.slne.surf.api.minestom.coroutine.minestomAsyncScope
import kotlinx.coroutines.CoroutineScope
import net.minestom.server.command.CommandSender
import net.minestom.server.command.ConsoleSender
import net.minestom.server.entity.Player

inline fun <SELF : CommandExecutable<SELF>> SELF.anyExecutor(
    crossinline executor: (CommandSender, CommandArguments) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Normal(ExecutorType.ANY) { info -> executor(info.sender, info.args) },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.playerExecutor(
    crossinline executor: (Player, CommandArguments) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Normal(ExecutorType.PLAYER) { info ->
        val player = info.sender
        check(player is Player) { "PLAYER executor received ${player::class.qualifiedName}" }
        executor(player, info.args)
    },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.consoleExecutor(
    crossinline executor: (ConsoleSender, CommandArguments) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Normal(ExecutorType.CONSOLE) { info ->
        val console = info.sender
        check(console is ConsoleSender) { "CONSOLE executor received ${console::class.qualifiedName}" }
        executor(console, info.args)
    },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.anyExecutionInfo(
    crossinline executor: (ExecutionInfo<CommandSender>) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Normal(ExecutorType.ANY) { info -> executor(info) },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.playerExecutionInfo(
    crossinline executor: (ExecutionInfo<Player>) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Normal(ExecutorType.PLAYER) { info ->
        val player = info.sender
        check(player is Player) { "PLAYER executor received ${player::class.qualifiedName}" }
        executor(ExecutionInfo(player, info.args, info.input))
    },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.consoleExecutionInfo(
    crossinline executor: (ExecutionInfo<ConsoleSender>) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Normal(ExecutorType.CONSOLE) { info ->
        val console = info.sender
        check(console is ConsoleSender) { "CONSOLE executor received ${console::class.qualifiedName}" }
        executor(ExecutionInfo(console, info.args, info.input))
    },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.anyResultingExecutor(
    crossinline executor: (CommandSender, CommandArguments) -> Int,
): SELF = addExecutor(
    ExecutorDefinition.Resulting(ExecutorType.ANY) { info -> executor(info.sender, info.args) },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.playerResultingExecutor(
    crossinline executor: (Player, CommandArguments) -> Int,
): SELF = addExecutor(
    ExecutorDefinition.Resulting(ExecutorType.PLAYER) { info ->
        val player = info.sender
        check(player is Player) { "PLAYER executor received ${player::class.qualifiedName}" }
        executor(player, info.args)
    },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.consoleResultingExecutor(
    crossinline executor: (ConsoleSender, CommandArguments) -> Int,
): SELF = addExecutor(
    ExecutorDefinition.Resulting(ExecutorType.CONSOLE) { info ->
        val console = info.sender
        check(console is ConsoleSender) { "CONSOLE executor received ${console::class.qualifiedName}" }
        executor(console, info.args)
    },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.anyResultingExecutionInfo(
    crossinline executor: (ExecutionInfo<CommandSender>) -> Int,
): SELF = addExecutor(
    ExecutorDefinition.Resulting(ExecutorType.ANY) { info -> executor(info) },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.playerResultingExecutionInfo(
    crossinline executor: (ExecutionInfo<Player>) -> Int,
): SELF = addExecutor(
    ExecutorDefinition.Resulting(ExecutorType.PLAYER) { info ->
        val player = info.sender
        check(player is Player) { "PLAYER executor received ${player::class.qualifiedName}" }
        executor(ExecutionInfo(player, info.args, info.input))
    },
)

inline fun <SELF : CommandExecutable<SELF>> SELF.consoleResultingExecutionInfo(
    crossinline executor: (ExecutionInfo<ConsoleSender>) -> Int,
): SELF = addExecutor(
    ExecutorDefinition.Resulting(ExecutorType.CONSOLE) { info ->
        val console = info.sender
        check(console is ConsoleSender) { "CONSOLE executor received ${console::class.qualifiedName}" }
        executor(ExecutionInfo(console, info.args, info.input))
    },
)

fun <SELF : CommandExecutable<SELF>> SELF.anyExecutorSuspend(
    scope: () -> CoroutineScope = { minestomAsyncScope },
    executor: suspend CoroutineScope.(CommandSender, CommandArguments) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Suspending(ExecutorType.ANY, scope) { info ->
        executor(info.sender, info.args)
    },
)

fun <SELF : CommandExecutable<SELF>> SELF.anyExecutorSuspend(
    scope: CoroutineScope,
    executor: suspend CoroutineScope.(CommandSender, CommandArguments) -> Unit,
): SELF = anyExecutorSuspend(scope = { scope }, executor = executor)

fun <SELF : CommandExecutable<SELF>> SELF.playerExecutorSuspend(
    scope: () -> CoroutineScope = { minestomAsyncScope },
    executor: suspend CoroutineScope.(Player, CommandArguments) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Suspending(ExecutorType.PLAYER, scope) { info ->
        val player = info.sender
        check(player is Player) { "PLAYER executor received ${player::class.qualifiedName}" }
        executor(player, info.args)
    },
)

fun <SELF : CommandExecutable<SELF>> SELF.playerExecutorSuspend(
    scope: CoroutineScope,
    executor: suspend CoroutineScope.(Player, CommandArguments) -> Unit,
): SELF = playerExecutorSuspend(scope = { scope }, executor = executor)

fun <SELF : CommandExecutable<SELF>> SELF.consoleExecutorSuspend(
    scope: () -> CoroutineScope = { minestomAsyncScope },
    executor: suspend CoroutineScope.(ConsoleSender, CommandArguments) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Suspending(ExecutorType.CONSOLE, scope) { info ->
        val console = info.sender
        check(console is ConsoleSender) { "CONSOLE executor received ${console::class.qualifiedName}" }
        executor(console, info.args)
    },
)

fun <SELF : CommandExecutable<SELF>> SELF.consoleExecutorSuspend(
    scope: CoroutineScope,
    executor: suspend CoroutineScope.(ConsoleSender, CommandArguments) -> Unit,
): SELF = consoleExecutorSuspend(scope = { scope }, executor = executor)

fun <SELF : CommandExecutable<SELF>> SELF.anyExecutionInfoExecutorSuspend(
    scope: () -> CoroutineScope = { minestomAsyncScope },
    executor: suspend CoroutineScope.(ExecutionInfo<CommandSender>) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Suspending(ExecutorType.ANY, scope) { info -> executor(info) },
)

fun <SELF : CommandExecutable<SELF>> SELF.anyExecutionInfoExecutorSuspend(
    scope: CoroutineScope,
    executor: suspend CoroutineScope.(ExecutionInfo<CommandSender>) -> Unit,
): SELF = anyExecutionInfoExecutorSuspend(scope = { scope }, executor = executor)

fun <SELF : CommandExecutable<SELF>> SELF.playerExecutionInfoExecutorSuspend(
    scope: () -> CoroutineScope = { minestomAsyncScope },
    executor: suspend CoroutineScope.(ExecutionInfo<Player>) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Suspending(ExecutorType.PLAYER, scope) { info ->
        val player = info.sender
        check(player is Player) { "PLAYER executor received ${player::class.qualifiedName}" }
        executor(ExecutionInfo(player, info.args, info.input))
    },
)

fun <SELF : CommandExecutable<SELF>> SELF.playerExecutionInfoExecutorSuspend(
    scope: CoroutineScope,
    executor: suspend CoroutineScope.(ExecutionInfo<Player>) -> Unit,
): SELF = playerExecutionInfoExecutorSuspend(scope = { scope }, executor = executor)

fun <SELF : CommandExecutable<SELF>> SELF.consoleExecutionInfoExecutorSuspend(
    scope: () -> CoroutineScope = { minestomAsyncScope },
    executor: suspend CoroutineScope.(ExecutionInfo<ConsoleSender>) -> Unit,
): SELF = addExecutor(
    ExecutorDefinition.Suspending(ExecutorType.CONSOLE, scope) { info ->
        val console = info.sender
        check(console is ConsoleSender) { "CONSOLE executor received ${console::class.qualifiedName}" }
        executor(ExecutionInfo(console, info.args, info.input))
    },
)

fun <SELF : CommandExecutable<SELF>> SELF.consoleExecutionInfoExecutorSuspend(
    scope: CoroutineScope,
    executor: suspend CoroutineScope.(ExecutionInfo<ConsoleSender>) -> Unit,
): SELF = consoleExecutionInfoExecutorSuspend(scope = { scope }, executor = executor)
