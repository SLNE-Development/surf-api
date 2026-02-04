@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.surfapi.velocity.api.command.executors

import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import dev.jorel.commandapi.VelocityExecutable
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.consoleExecutor
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
import kotlinx.coroutines.*
import net.kyori.adventure.text.logger.slf4j.ComponentLogger

/**
 * Default [CoroutineExceptionHandler] used by [defaultScope].
 *
 * This handler logs any uncaught exception from command coroutines (except [CancellationException]),
 * which can happen when a coroutine fails outside of the explicit try/catch in executor wrappers.
 */
private val defaultScopeExceptionHandler = CoroutineExceptionHandler { _, t ->
    if (t is CancellationException) return@CoroutineExceptionHandler
    ComponentLogger.logger("Velocity Command Executor")
        .atError()
        .setCause(t)
        .log("Uncaught exception in command coroutine")
}

/**
 * Default coroutine scope used for Velocity command executors.
 *
 * Uses a bounded IO dispatcher ([CoroutineDispatcher.limitedParallelism]) with a [SupervisorJob] so one failing
 * command does not cancel others. A [CoroutineName] is applied for easier debugging and logs.
 *
 * This is intended as a safe default when no plugin-managed scope is available.
 */
@PublishedApi
@InternalSurfApi
internal val defaultScope = CoroutineScope(
    Dispatchers.IO.limitedParallelism(16) // We are not planning to do blocking work but preparing in case a developer messes up
            + SupervisorJob()
            + CoroutineName("Velocity Default Command Executor Scope")
            + defaultScopeExceptionHandler
)

/**
 * Registers an executor that can be run by any [CommandSource] and executes it inside a coroutine.
 *
 * The executor is launched in the provided [CoroutineScope]. Any thrown exceptions are handled
 * consistently and reported back to the sender if applicable.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the [defaultScope].
 * @param executor The suspending executor logic.
 */
inline fun VelocityExecutable<*>.anyExecutorSuspend(
    scope: CoroutineScope = defaultScope,
    crossinline executor: suspend CoroutineScope.(CommandSource, CommandArguments) -> Unit
) = anyExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Registers an executor that can only be run by a [Player] and executes it inside a coroutine.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the [defaultScope].
 * @param executor The suspending executor logic.
 */
inline fun VelocityExecutable<*>.playerExecutorSuspend(
    scope: CoroutineScope = defaultScope,
    crossinline executor: suspend CoroutineScope.(Player, CommandArguments) -> Unit
) = playerExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Registers an executor that can only be run by the console and executes it inside a coroutine.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the [defaultScope].
 * @param executor The suspending executor logic.
 */
inline fun VelocityExecutable<*>.consoleExecutorSuspend(
    scope: CoroutineScope = defaultScope,
    crossinline executor: suspend CoroutineScope.(ConsoleCommandSource, CommandArguments) -> Unit
) = consoleExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Launches a command executor in this [CoroutineScope] and applies unified exception handling.
 *
 * [CancellationException] is rethrown to respect coroutine cancellation semantics.
 * All other exceptions are forwarded to [handleCommandFailure].
 *
 * @param sender The command sender that executed the command.
 * @param block The suspending execution logic.
 */
@PublishedApi
@InternalSurfApi
internal inline fun <S : CommandSource> CoroutineScope.launchCommandExecutor(
    sender: S,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    launch {
        try {
            block()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            handleCommandFailure(sender, t)
        }
    }
}

/**
 * Handles failures that occur during command execution.
 *
 * Syntax-related exceptions are sent directly to the sender.
 * Any other exception is logged and a generic error message is sent.
 *
 * @param sender The command sender.
 * @param t The thrown exception.
 */
@Suppress("NOTHING_TO_INLINE")
@PublishedApi
@InternalSurfApi
internal inline fun handleCommandFailure(sender: CommandSource, t: Throwable) {
    when (t) {
        is WrapperCommandSyntaxException -> sender.sendSyntaxMessageOrRethrow(t.message, t)
        is CommandSyntaxException -> sender.sendSyntaxMessageOrRethrow(t.message, t)
        else -> {
            ComponentLogger.logger()
                .atError()
                .setCause(t)
                .log("Failed to execute command")
            sender.sendMessage(text("Failed to execute command", Colors.ERROR))
        }
    }
}

/**
 * Sends a syntax error message to the sender or rethrows the exception
 * if no message is available.
 *
 * @param message The error message.
 * @param t The originating exception.
 */
@PublishedApi
@InternalSurfApi
internal fun CommandSource.sendSyntaxMessageOrRethrow(message: String?, t: Throwable) {
    val msg = message ?: throw t
    sendMessage(text(msg, Colors.ERROR))
}