package dev.slne.surf.api.paper.hook.papi

import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfApiBean
import dev.slne.surf.api.spring.surfBean

/**
 * Registers PlaceholderAPI expansions built by hand.
 *
 * Classes annotated with [dev.slne.surf.api.paper.hook.papi.PapiExpansion] need no call here;
 * surf-api registers them with their plugin. Without PlaceholderAPI installed, registration only
 * reserves the identifier.
 *
 * Identifiers are unique across every registered expansion, ignoring case. An expansion
 * registered before surf-api enables reaches PlaceholderAPI when surf-api enables, and every
 * expansion still registered is removed from PlaceholderAPI when surf-api disables.
 *
 * PlaceholderAPI fires a synchronous Bukkit event on registration, so call [register] and
 * [unregister] on the main thread.
 */
@SurfApiBean
interface SurfPaperPAPIHook {

    /**
     * Registers [expansion] under its identifier.
     *
     * @param expansion the expansion to register
     * @throws IllegalArgumentException if an expansion with the same identifier is registered
     * @throws IllegalStateException if PlaceholderAPI rejects the expansion; it is not registered
     * then
     */
    fun register(expansion: PapiExpansion)

    /**
     * Unregisters [expansion]; does nothing if this instance is not registered.
     *
     * @param expansion the expansion to unregister
     */
    fun unregister(expansion: PapiExpansion)

    companion object {
        @InternalSurfApi
        val instance by surfBean<SurfPaperPAPIHook>()
    }
}
