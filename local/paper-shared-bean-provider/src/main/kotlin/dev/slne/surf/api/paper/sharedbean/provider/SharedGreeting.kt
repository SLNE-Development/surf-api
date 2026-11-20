package dev.slne.surf.api.paper.sharedbean.provider

import dev.slne.surf.api.spring.PluginApi

/**
 * The contract the providing plugin exports for another plugin to inject.
 *
 * It is declared here, in the providing plugin's own jar, and nowhere else. A consumer reaches it
 * only through Paper's plugin class loader group, so the `Class` object the consumer casts to is the
 * one this jar's loader defined.
 */
@PluginApi
interface SharedGreeting {

    /**
     * Returns [GREETING].
     */
    fun greet(): String

    companion object {

        /**
         * The exact text [greet] returns. Only this plugin's implementation produces it, so finding
         * it in a line the consumer logged proves a call across the plugin boundary.
         */
        const val GREETING = "one-shared-greeting-2f91c7"
    }
}
