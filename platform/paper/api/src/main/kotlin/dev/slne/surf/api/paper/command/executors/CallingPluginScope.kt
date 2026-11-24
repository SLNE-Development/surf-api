package dev.slne.surf.api.paper.command.executors

import dev.jorel.commandapi.CommandAPI
import dev.slne.surf.api.paper.plugin.SurfPaperPluginRuntime
import dev.slne.surf.api.shared.api.util.PublishedSurfApi
import kotlinx.coroutines.CoroutineScope
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

/**
 * Returns the scope that the suspending commands, requirements and arguments of the calling
 * plugin run in: the plugin's coroutine scope with its main dispatcher, so that their code starts
 * on the server thread.
 *
 * The calling plugin is the one that provides the first class on the stack outside surf-api's
 * command API and outside CommandAPI. It is determined right away, when a command is declared;
 * the scope itself is resolved on first use, when the plugin has loaded.
 *
 * Downstream code inlines calls to this function through the default arguments of the suspending
 * executor DSL, so its name and signature are part of surf-api's binary interface.
 *
 * @throws IllegalStateException if no plugin is on the stack
 */
@PublishedApi
@PublishedSurfApi
internal fun callingPluginCommandScope(): CoroutineScopeProvider {
    val plugin = callingPlugin()
    val scope by lazy {
        val runtime = SurfPaperPluginRuntime.get()
        CoroutineScope(
            runtime.coroutineScope(plugin).coroutineContext + runtime.dispatchers(plugin).main,
        )
    }

    return { scope }
}

private fun callingPlugin(): Plugin {
    val commandApiPlugin = JavaPlugin.getProvidingPlugin(CommandAPI::class.java)

    return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk { frames ->
        frames
            .map { it.declaringClass }
            .filter { !it.name.startsWith(COMMAND_API_PACKAGE) }
            .map { runCatching { JavaPlugin.getProvidingPlugin(it) }.getOrNull() }
            .filter { it != null && it != commandApiPlugin }
            .findFirst()
            .orElseThrow {
                IllegalStateException(
                    "No plugin declares this command: no plugin class is on the stack",
                )
            }
    }!!
}

/**
 * The package of surf-api's command API, whose frames belong to whichever plugin declares the
 * command.
 */
private const val COMMAND_API_PACKAGE = "dev.slne.surf.api.paper.command."
