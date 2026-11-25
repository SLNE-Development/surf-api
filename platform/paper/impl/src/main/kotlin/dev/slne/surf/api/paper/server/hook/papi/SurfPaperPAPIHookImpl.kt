package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.hook.papi.SurfPaperPAPIHook
import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion
import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.stereotype.Service
import java.util.*

/**
 * Holds every registered expansion and attaches it to PlaceholderAPI while surf-api is enabled.
 *
 * Registrations made before surf-api's `onEnable` are attached there, where a rejected expansion
 * is logged and stays registered; `onDisable` detaches and forgets every registration still held.
 * Identifiers are keyed ignoring case, as PlaceholderAPI keys them. One lock guards the registry
 * and the enabled state.
 */
@Service
class SurfPaperPAPIHookImpl internal constructor(
    private val holders: ExpansionHolderFactory,
) : SurfPaperPAPIHook, SurfLifecycle {

    private val lock = Any()
    private val registrations = HashMap<String, Registration>()
    private var isEnabled = false

    override fun register(expansion: PapiExpansion) {
        register(ManualExpansionResolver.define(expansion))
    }

    override fun unregister(expansion: PapiExpansion) {
        unregister(expansion.identifier, owner = expansion)
    }

    /**
     * Registers [definition] and attaches it right away while surf-api is enabled.
     *
     * @param definition the expansion to register
     * @throws IllegalArgumentException if an expansion with the same identifier is registered
     * @throws IllegalStateException if PlaceholderAPI rejects the expansion; it is not registered
     * then
     */
    internal fun register(definition: ExpansionDefinition) {
        synchronized(lock) {
            val key = definition.identifier.registryKey()
            require(key !in registrations) {
                "An expansion with identifier '${definition.identifier}' is already registered"
            }

            val holder = holders.create(definition)

            if (isEnabled) {
                holder.attach()
            }

            registrations[key] = Registration(definition, holder)
        }
    }

    /**
     * Unregisters the expansion [identifier] if [owner] declared it.
     *
     * @param identifier the expansion's identifier
     * @param owner the [ExpansionDefinition.owner] the registration must have
     */
    internal fun unregister(identifier: String, owner: Any) {
        synchronized(lock) {
            val key = identifier.registryKey()
            val registration = registrations[key] ?: return
            if (registration.definition.owner !== owner) return

            registrations.remove(key)

            if (isEnabled) {
                registration.holder.detach()
            }
        }
    }

    override suspend fun onEnable() {
        synchronized(lock) {
            for (registration in registrations.values) {
                attachLoggingFailure(registration)
            }

            isEnabled = true
        }
    }

    override suspend fun onDisable() {
        synchronized(lock) {
            isEnabled = false

            for (registration in registrations.values) {
                detachLoggingFailure(registration)
            }

            registrations.clear()
        }
    }

    private fun attachLoggingFailure(registration: Registration) {
        try {
            registration.holder.attach()
        } catch (exception: Exception) {
            log.atSevere()
                .withCause(exception)
                .log(
                    "Failed to register the PlaceholderAPI expansion '%s'",
                    registration.definition.identifier,
                )
        }
    }

    private fun detachLoggingFailure(registration: Registration) {
        try {
            registration.holder.detach()
        } catch (exception: Exception) {
            log.atWarning()
                .withCause(exception)
                .log(
                    "Failed to unregister the PlaceholderAPI expansion '%s'",
                    registration.definition.identifier,
                )
        }
    }

    private fun String.registryKey(): String = lowercase(Locale.ROOT)

    private class Registration(val definition: ExpansionDefinition, val holder: ExpansionHolder)

    private companion object {
        val log = logger()
    }
}
