@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.surfapi.bukkit.api.command.executors

import com.github.shynixn.mccoroutine.folia.SuspendingPlugin
import com.github.shynixn.mccoroutine.folia.scope
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.jorel.commandapi.BukkitExecutable
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.adventure.text
import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bukkit.command.*
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Registers an executor that can be run by any [CommandSender] and executes it inside a coroutine.
 *
 * The executor is launched in the provided [CoroutineScope]. Any thrown exceptions are handled
 * consistently and reported back to the sender if applicable.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the calling plugin's scope.
 * @param executor The suspending executor logic.
 */
inline fun BukkitExecutable<*>.anyExecutorSuspend(
    scope: CoroutineScope = extractCallingPluginScopeOrThrow(),
    crossinline executor: suspend CoroutineScope.(CommandSender, CommandArguments) -> Unit
) = anyExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Registers an executor that can only be run by a [Player] and executes it inside a coroutine.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the calling plugin's scope.
 * @param executor The suspending executor logic.
 */
inline fun BukkitExecutable<*>.playerExecutorSuspend(
    scope: CoroutineScope = extractCallingPluginScopeOrThrow(),
    crossinline executor: suspend CoroutineScope.(Player, CommandArguments) -> Unit
) = playerExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Registers an executor that can only be run by an [Entity] and executes it inside a coroutine.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the calling plugin's scope.
 * @param executor The suspending executor logic.
 */
inline fun BukkitExecutable<*>.entityExecutorSuspend(
    scope: CoroutineScope = extractCallingPluginScopeOrThrow(),
    crossinline executor: suspend CoroutineScope.(Entity, CommandArguments) -> Unit
) = entityExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Registers an executor that can only be run by the console and executes it inside a coroutine.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the calling plugin's scope.
 * @param executor The suspending executor logic.
 */
inline fun BukkitExecutable<*>.consoleExecutorSuspend(
    scope: CoroutineScope = extractCallingPluginScopeOrThrow(),
    crossinline executor: suspend CoroutineScope.(ConsoleCommandSender, CommandArguments) -> Unit
) = consoleExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Registers an executor that can only be run by a command block and executes it inside a coroutine.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the calling plugin's scope.
 * @param executor The suspending executor logic.
 */
inline fun BukkitExecutable<*>.commandBlockExecutorSuspend(
    scope: CoroutineScope = extractCallingPluginScopeOrThrow(),
    crossinline executor: suspend CoroutineScope.(BlockCommandSender, CommandArguments) -> Unit
) = commandBlockExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Registers an executor that can only be run by a [ProxiedCommandSender] and executes it inside a coroutine.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the calling plugin's scope.
 * @param executor The suspending executor logic.
 */
inline fun BukkitExecutable<*>.proxyExecutorSuspend(
    scope: CoroutineScope = extractCallingPluginScopeOrThrow(),
    crossinline executor: suspend CoroutineScope.(ProxiedCommandSender, CommandArguments) -> Unit
) = proxyExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Registers an executor that can only be run by a [NativeProxyCommandSender] and executes it inside a coroutine.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the calling plugin's scope.
 * @param executor The suspending executor logic.
 */
inline fun BukkitExecutable<*>.nativeExecutorSuspend(
    scope: CoroutineScope = extractCallingPluginScopeOrThrow(),
    crossinline executor: suspend CoroutineScope.(NativeProxyCommandSender, CommandArguments) -> Unit
) = nativeExecutor { sender, arguments ->
    scope.launchCommandExecutor(sender) {
        executor(sender, arguments)
    }
}

/**
 * Registers an executor that can only be run by a remote console and executes it inside a coroutine.
 *
 * @param scope The [CoroutineScope] used to launch the executor. Defaults to the calling plugin's scope.
 * @param executor The suspending executor logic.
 */
inline fun BukkitExecutable<*>.remoteConsoleExecutorSuspend(
    scope: CoroutineScope = extractCallingPluginScopeOrThrow(),
    crossinline executor: suspend CoroutineScope.(RemoteConsoleCommandSender, CommandArguments) -> Unit
) = remoteConsoleExecutor { sender, arguments ->
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
internal inline fun <S : CommandSender> CoroutineScope.launchCommandExecutor(
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
internal inline fun handleCommandFailure(sender: CommandSender, t: Throwable) {
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
internal fun CommandSender.sendSyntaxMessageOrRethrow(message: String?, t: Throwable) {
    val msg = message ?: throw t
    sendMessage(text(msg, Colors.ERROR))
}

/**
 * Extracts the [CoroutineScope] of the calling plugin.
 *
 * The calling plugin must implement [SuspendingPlugin]. Otherwise, an exception is thrown.
 *
 * @return The plugin's coroutine scope.
 * @throws IllegalArgumentException If the plugin does not implement [SuspendingPlugin].
 */
@Suppress("NOTHING_TO_INLINE")
@PublishedApi
@InternalSurfApi
internal inline fun Any.extractCallingPluginScopeOrThrow(): CoroutineScope {
    val plugin = JavaPlugin.getProvidingPlugin(javaClass)
    require(plugin is SuspendingPlugin) {
        "Failed to extract CoroutineScope: plugin '${plugin.name}' does not implement SuspendingPlugin. " +
                "Provide a CoroutineScope explicitly or use a SuspendingPlugin."
    }

    return plugin.scope
}