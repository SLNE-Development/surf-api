package dev.slne.surf.api.spring.context

import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext

/**
 * Takes part in a plugin's enable and disable phases, around the plugin's own lifecycle hooks.
 *
 * Beans of a plugin's full context that implement it are called by [PluginContext]: [afterEnable]
 * once every `onEnable` hook of the plugin has run, [beforeDisable] before the first `onDisable`
 * hook runs. A surf-api module contributes such a bean through a [ContextContributor] to start
 * serving a plugin's endpoints only once the plugin is enabled, and to stop accepting work while
 * the plugin's beans are still alive. A root context that hosts an application calls the listeners
 * defined in it the same way.
 *
 * A failing [afterEnable] fails the enable phase. A failing [beforeDisable] is logged and does not
 * keep the other listeners or the disable hooks from running.
 */
interface ContextPhaseListener {

    /**
     * Runs after every `onEnable` hook of the plugin.
     */
    suspend fun afterEnable() {}

    /**
     * Runs before the first `onDisable` hook of the plugin.
     */
    suspend fun beforeDisable() {}
}

/**
 * Runs [ContextPhaseListener.afterEnable] of every listener defined in this context itself.
 */
internal suspend fun ApplicationContext.runAfterEnable() {
    getBeansOfType<ContextPhaseListener>().values.forEach { it.afterEnable() }
}

/**
 * Runs [ContextPhaseListener.beforeDisable] of every listener defined in this context itself; a
 * failing listener is logged under [owner] and does not stop the others.
 */
internal suspend fun ApplicationContext.runBeforeDisable(owner: String) {
    getBeansOfType<ContextPhaseListener>().values.forEachLoggingFailures(
        logFailure = { listener, exception -> logBeforeDisableFailure(listener, owner, exception) },
    ) { it.beforeDisable() }
}

private fun logBeforeDisableFailure(listener: ContextPhaseListener, owner: String, exception: Throwable) {
    LogFactory.getLog(ContextPhaseListener::class.java)
        .error("beforeDisable of ${listener.javaClass.name} in '$owner' failed", exception)
}
