package dev.slne.surf.api.minestom.plugin

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asContextElement
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import java.nio.file.Path

/**
 * The main class of a plugin the server loads from its `plugins` directory.
 *
 * ```
 * class ExamplePlugin : SurfMinestomPlugin() {
 *     override suspend fun onEnable() {
 *         eventNode.addListener<PlayerJoinEvent> { it.player.sendMessage(text("Hello!")) }
 *         commandAPICommand("hello") { playerExecutor { player, _ -> player.sendMessage(text("Hi")) } }
 *     }
 * }
 * ```
 *
 * The class needs a constructor without parameters, or has to be an `object`. It is named as
 * `main` in the plugin's `minestom-plugin.json`.
 *
 * Listeners registered on [eventNode], coroutines launched in [scope] and commands registered while
 * one of the lifecycle functions runs, or from within [scope], belong to the plugin and are
 * removed once it is disabled.
 */
abstract class SurfMinestomPlugin {
    private var context: Context? = null

    private val requireContext: Context
        get() = checkNotNull(context) { "${javaClass.name} has not been initialized by the server yet" }

    /** What the plugin's `minestom-plugin.json` says about it. */
    val meta: PluginMeta get() = requireContext.meta

    /** The plugin's own event node; removed with all its listeners when the plugin is disabled. */
    val eventNode: EventNode<Event> get() = requireContext.eventNode

    /** The plugin's own directory, `plugins/<id>`, for its configuration and data. */
    val dataDirectory: Path get() = requireContext.dataDirectory

    val logger: ComponentLogger get() = requireContext.logger

    /** A scope for the plugin's coroutines; cancelled when the plugin is disabled. */
    val scope: CoroutineScope get() = requireContext.scope

    /** Called while the server starts, before it accepts players. */
    open suspend fun onLoad() = Unit

    /** Called on the first tick after the server started. */
    open suspend fun onEnable() = Unit

    /** Called when the plugin is disabled, at the latest when the server shuts down. */
    open suspend fun onDisable() = Unit

    /** Initializes the plugin; called once by the server right after creating it. */
    @InternalSurfApi
    fun initialize(context: Context) {
        check(this.context == null) { "${javaClass.name} is already initialized" }
        this.context = context
    }

    override fun toString(): String = context?.let { "${it.meta.name} ${it.meta.version}" } ?: javaClass.name

    /** What the server hands a plugin when creating it. */
    @InternalSurfApi
    class Context(
        val meta: PluginMeta,
        val eventNode: EventNode<Event>,
        val dataDirectory: Path,
        val logger: ComponentLogger,
        val scope: CoroutineScope,
    )

    companion object {
        private val current = ThreadLocal<SurfMinestomPlugin?>()

        /**
         * The plugin whose code is running on this thread, or `null` when it is not a plugin's.
         *
         * Known while a lifecycle function runs and within the plugin's [scope].
         */
        @InternalSurfApi
        fun current(): SurfMinestomPlugin? = current.get()

        /** A coroutine context element that makes [plugin] the [current] one. */
        @InternalSurfApi
        fun contextElement(plugin: SurfMinestomPlugin): ThreadContextElement<SurfMinestomPlugin?> =
            current.asContextElement(plugin)
    }
}
