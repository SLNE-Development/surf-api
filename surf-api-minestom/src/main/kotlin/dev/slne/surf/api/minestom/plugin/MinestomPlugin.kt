package dev.slne.surf.api.minestom.plugin

import com.google.inject.AbstractModule
import dev.slne.surf.api.minestom.command.CommandRegistrar
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.plugin.annotation.DataDirectory
import dev.slne.surf.api.minestom.plugin.annotation.MinestomPluginMeta
import org.jetbrains.annotations.ApiStatus

/**
 * A plugin as the server sees it: a Guice module plus the entrypoint that starts it.
 *
 * Plugins are discovered through `ServiceLoader`, so a subclass needs a
 * `META-INF/services/dev.slne.surf.api.minestom.plugin.MinestomPlugin` entry, a
 * [MinestomPluginMeta] annotation for its id and dependencies, and a no-argument constructor.
 *
 * Each plugin is wired into its own private section of the server's injector. Everything the server
 * binds stays injectable and keeps sharing the server's instances - services, the server's worlds,
 * Minestom's own types. What the plugin binds is its own: two plugins may bind the same key without
 * colliding, and each one gets its own [DataDirectory] path.
 */
abstract class MinestomPlugin(
    val entrypoint: Class<out MinestomPluginEntrypoint>
) : AbstractModule() {
    val meta = MinestomPluginMeta.get(javaClass)

    private val declaredEventRegistrars = linkedSetOf<Class<out EventRegistrar>>()
    private val declaredCommandRegistrars = linkedSetOf<Class<out CommandRegistrar>>()

    @get:ApiStatus.Internal
    val eventRegistrars: Set<Class<out EventRegistrar>>
        get() = declaredEventRegistrars

    @get:ApiStatus.Internal
    val commandRegistrars: Set<Class<out CommandRegistrar>>
        get() = declaredCommandRegistrars

    init {
        MinestomPluginMeta.validate(meta)
    }

    final override fun configure() {
        bind(entrypoint)

        configurePlugin()
    }

    /**
     * Register the plugin's Guice bindings here.
     *
     * ```
     * override fun configurePlugin() {
     *     bind(MyService::class.java).asEagerSingleton()
     *     bindEventRegistrar<MyListener>()
     *     bindCommandRegistrar<MyCommands>()
     * }
     * ```
     *
     * This method must not start services, open connections or register
     * listeners. Side effects belong in the entrypoint's start method.
     *
     * The entrypoint is already bound when this runs, so binding it again is an error.
     */
    protected open fun configurePlugin() = Unit

    /**
     * Registers [registrar] as an [EventRegistrar], so the server hands it its event node during
     * startup.
     */
    protected fun bindEventRegistrar(registrar: Class<out EventRegistrar>) {
        bind(registrar)
        declaredEventRegistrars += registrar
    }

    /**
     * Registers [T] as an [EventRegistrar], so the server hands it its event node during startup.
     *
     * ```
     * bindEventRegistrar<WelcomeListener>()
     * ```
     */
    protected inline fun <reified T : EventRegistrar> bindEventRegistrar() =
        bindEventRegistrar(T::class.java)

    /**
     * Registers [registrar] as a [CommandRegistrar], so the server asks it to register its
     * commands during startup.
     */
    protected fun bindCommandRegistrar(registrar: Class<out CommandRegistrar>) {
        bind(registrar)
        declaredCommandRegistrars += registrar
    }

    /**
     * Registers [T] as a [CommandRegistrar], so the server asks it to register its commands during
     * startup.
     *
     * ```
     * bindCommandRegistrar<MyCommands>()
     * ```
     */
    protected inline fun <reified T : CommandRegistrar> bindCommandRegistrar() =
        bindCommandRegistrar(T::class.java)
}
