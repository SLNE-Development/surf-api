package dev.slne.surf.api.velocity.sharedbean

import com.google.inject.Inject
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import dev.slne.surf.api.velocity.plugin.SurfVelocityPlugin
import dev.slne.surf.api.velocity.sharedbean.consumer.SharedBeanReport

/**
 * The consumer's main class.
 *
 * It lives in the parent package of both plugins on purpose: the component scan starts there and
 * must still pick up only this jar's components, not the provider's. Its constructor parameter and
 * initialized property let the boot test check that the context is built after the constructor.
 */
@Plugin(
    id = SharedBeanReport.PLUGIN_ID,
    name = "Surf Shared Bean Consumer",
    version = "1.0.0",
    description = "Injects another plugin's @PluginApi bean for surf-api's Velocity boot test.",
    authors = ["SLNE Development Team"],
    dependencies = [
        Dependency(id = "surf-api-velocity"),
        Dependency(id = "surf-shared-bean-provider"),
        // The command bean extends CommandAPICommand, and the LuckPerms adapter refers to its API.
        Dependency(id = "commandapi"),
        Dependency(id = "luckperms"),
    ],
)
class SharedBeanConsumerPlugin @Inject constructor(val proxy: ProxyServer) : SurfVelocityPlugin() {

    /**
     * Set by the constructor; `null` while the context loads would mean it loaded too early.
     */
    val constructedMarker: String = SharedBeanReport.PLUGIN_ID

    /**
     * Injected by Guice as a field; the boot test reports whether it is set during the load phase.
     */
    @field:Inject
    var injectedContainer: PluginContainer? = null

    /**
     * Whether Guice has called [injectMethod] yet.
     */
    var methodInjected: Boolean = false
        private set

    /**
     * Injected by Guice as a method; the boot test reports whether it ran before the load phase.
     */
    @Inject
    fun injectMethod() {
        methodInjected = true
    }
}
