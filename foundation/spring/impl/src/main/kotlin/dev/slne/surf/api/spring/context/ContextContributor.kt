package dev.slne.surf.api.spring.context

import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 * Adds bean definitions to the full context of every plugin before the context refreshes.
 *
 * Contributors are beans of surf-api's root context, so a surf-api module plugs a feature into
 * every plugin context by declaring one, such as the config integration that registers a
 * `ConfigHandle` per `@SurfConfig` class. [SurfContextManager] calls each contributor after the
 * plugin's components are scanned and before the platform registers its own beans. Bootstrap
 * contexts get no contributions: the root context, and with it the contributors, does not exist
 * yet in Paper's bootstrap phase.
 *
 * A root context that hosts an application in the same context, such as a standalone service,
 * finds its contributors through `java.util.ServiceLoader` instead, because no bean exists before its
 * refresh; a contributor is therefore also listed in `META-INF/services` and has a constructor
 * without parameters.
 */
fun interface ContextContributor {

    /**
     * Registers bean definitions for [plugin] in [context].
     */
    fun contribute(context: AnnotationConfigApplicationContext, plugin: PluginDescriptor)
}
