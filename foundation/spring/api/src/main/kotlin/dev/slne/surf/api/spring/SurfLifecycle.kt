package dev.slne.surf.api.spring

/**
 * Receives the platform lifecycle phases of the plugin whose context holds this bean.
 *
 * Each hook defaults to doing nothing. Within one context, [onLoad] and [onEnable] run in the order
 * in which the beans finished initializing, so a bean's dependencies run before the bean itself;
 * [onDisable] runs in the reverse order. `@Order` or [org.springframework.core.Ordered] moves a
 * bean ahead of the beans that declare no order.
 *
 * The hooks of one phase run one after another, never concurrently. A hook that throws aborts the
 * phase and fails the plugin, except in [onDisable], where the failure is logged and the remaining
 * hooks still run.
 *
 * Only beans that exist when a phase starts take part in it; a lazy bean created later misses the
 * phases that already ran.
 *
 * On Paper the hooks block the thread that calls the phase, which is the main thread for load,
 * enable and disable. A hook may wait for work dispatched through `PaperDispatchers.main`: while
 * the phase runs, the blocked thread runs that work itself, and on a server without regionized
 * ticking the same holds for `region` and `entity` work. A hook must not wait for region or entity
 * work on a regionized server such as Folia, nor for tasks handed to the Bukkit scheduler directly;
 * such work may only be launched without waiting for it.
 */
interface SurfLifecycle {

    /**
     * Runs while the platform loads the plugin, after every bean of its context exists.
     */
    suspend fun onLoad() {}

    /**
     * Runs while the platform enables the plugin.
     *
     * Listeners and commands of the context are registered at this point, and the server is
     * available.
     */
    suspend fun onEnable() {}

    /**
     * Runs while the platform disables the plugin, before its context closes.
     */
    suspend fun onDisable() {}
}

/**
 * Receives Paper's bootstrap phase; on other platforms it runs right before `onLoad`.
 *
 * Velocity, Minestom and the standalone runtime have no bootstrap phase: the beans live in the
 * plugin's or root's single context, and [onBootstrap] runs when that context loads, before any
 * [SurfLifecycle.onLoad] of it. The restrictions below apply to Paper only.
 *
 * On Paper a plugin gets a bootstrap context in the bootstrap phase, when the plugin sees only the
 * classes of its bootstrap dependencies and Paper has not created the plugin instance yet. It holds
 * the plugin's components that implement this interface or carry [BootstrapComponent]; their
 * dependencies must come from surf-api or be such components themselves. The plugin's full context
 * is built in the load phase below it and can inject every bootstrap bean.
 *
 * A bootstrap bean must not depend on the API of a plugin that is not a bootstrap dependency, such
 * as LuckPerms, nor on the concrete plugin class; it may hold the plugin as
 * `org.bukkit.plugin.Plugin`, which works from the load phase on. No server exists yet: the Bukkit
 * server API is unavailable, and the main and region threads do not tick, so dispatching to them
 * never completes. Hooks run in initialization order, `@Order` first, like those of
 * [SurfLifecycle].
 */
interface SurfBootstrapLifecycle {

    /**
     * Runs right after the context that holds this bean was built, before its load hooks.
     */
    suspend fun onBootstrap()
}
